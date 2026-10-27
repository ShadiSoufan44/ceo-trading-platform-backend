package com.ceo.trading_platform_backend.services;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ceo.trading_platform_backend.dto.OrderResponseDTO;
import com.ceo.trading_platform_backend.dto.AdminOrderResponseDTO;
import com.ceo.trading_platform_backend.dto.OrderHistoryEntryDTO;
import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.exception.ResourceNotFoundException;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.OrderStatusChange;
import com.ceo.trading_platform_backend.repositories.InstrumentRepository;
import com.ceo.trading_platform_backend.repositories.HoldingRepository;
import com.ceo.trading_platform_backend.repositories.OrderRepository;
import com.ceo.trading_platform_backend.repositories.UserRepository;

/**
 * Read-only, system-wide order queries for the admin controller.
 * Status history supplies recorded placement/current-status information, not a full settlement ledger.
 */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class AdminService {

    // PostgreSQL compares UUIDs by unsigned bytes, equivalent to canonical UUID text order.
    // A UUID tie-break is deterministic, but cannot prove event chronology for equal timestamps.
    private static final Comparator<OrderStatusChange> HISTORY_ORDER = Comparator
            .comparing(OrderStatusChange::getDate, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(change -> change.getOrderStatusChangeId() == null
                    ? "" : change.getOrderStatusChangeId().toString());
    private static final Set<OrderStatus> RESOLVED_STATUSES = Set.of(
            OrderStatus.FULFILLED, OrderStatus.REJECTED, OrderStatus.CANCELLED);

    private final OrderRepository orderRepository;
    private final InstrumentRepository instrumentRepository;
    private final HoldingRepository holdingRepository;
    private final UserRepository userRepository;

    public AdminService(OrderRepository orderRepository, InstrumentRepository instrumentRepository,
            HoldingRepository holdingRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.instrumentRepository = instrumentRepository;
        this.holdingRepository = holdingRepository;
        this.userRepository = userRepository;
    }

    /** All filters are optional and combined in the database before applying pagination. */
    public Page<AdminOrderResponseDTO> getOrders(UUID userId, String userName, OrderStatus status,
            LocalDate from, LocalDate to, String ticker, int page, int size) {
        if (page < 0) {
            throw badRequest("page must be zero or greater");
        }
        if (size < 1 || size > 100) {
            throw badRequest("size must be between 1 and 100");
        }
        if ((long) page * size > Integer.MAX_VALUE) {
            throw badRequest("page offset exceeds the supported range");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw badRequest("from must be on or before to");
        }
        String name = normalize(userName, "userName");
        String symbol = normalize(ticker, "ticker");
        // Treat %, _ and the escape character as literal parts of a person's name.
        String namePattern = name == null ? null : "%" + name.toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        Instant fromUtc;
        Instant toExclusiveUtc;
        try {
            fromUtc = from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC);
            toExclusiveUtc = to == null ? null : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeException ex) {
            throw badRequest("date range is outside the supported range");
        }

        PageRequest pageable = PageRequest.of(page, size);
        Page<Order> orders = orderRepository.searchAdminOrders(userId, namePattern,
                status == null ? null : status.name(), fromUtc, toExclusiveUtc,
                symbol == null ? null : symbol.toUpperCase(Locale.ROOT), pageable);
        if (orders.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, orders.getTotalElements());
        }

        // REPEATABLE_READ keeps filtering, totals, histories and instruments on one database snapshot.
        // Bulk fetch histories and instruments for this page, avoiding per-row lookups.
        Map<UUID, Order> hydratedOrders = orderRepository.findWithHistoryByOrderIdIn(
                orders.stream().map(Order::getOrderId).toList()).stream()
                .collect(Collectors.toMap(Order::getOrderId, Function.identity()));
        Map<UUID, Instrument> instruments = loadInstruments(orders.getContent());
        Map<UUID, List<Holding>> settlements = loadSettlements(orders.getContent());
        Map<UUID, String> clientNames = loadClientNames(orders.getContent());
        return orders.map(order -> toAdminResponse(hydratedOrders.getOrDefault(order.getOrderId(), order),
            instruments, settlements, clientNames));
    }

    /** Exact order lookup across all clients; missing orders produce 404 via the exception handler. */
    public OrderResponseDTO getOrderById(UUID orderId) {
        Order order = findOrderWithHistory(orderId);
        return toResponse(order, loadInstruments(List.of(order)));
    }

    public AdminOrderResponseDTO getAdminOrderById(UUID orderId) {
        Order order = findOrderWithHistory(orderId);
        return toAdminResponse(order, loadInstruments(List.of(order)), loadSettlements(List.of(order)),
            loadClientNames(List.of(order)));
    }

    /** Every recorded event for an order; an existing order without history returns an empty list. */
    public List<OrderHistoryEntryDTO> getOrderHistory(UUID orderId) {
        return historyEntries(List.of(findOrderWithHistory(orderId)));
    }

    /** Every recorded event across this client's orders. No matching entries returns an empty list. */
    public List<OrderHistoryEntryDTO> getUserOrderHistory(UUID userId) {
        return historyEntries(orderRepository.findWithHistoryByClientId(userId));
    }

    private Order findOrderWithHistory(UUID orderId) {
        return orderRepository.findWithHistoryByOrderIdIn(List.of(orderId)).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
    }

    private List<OrderHistoryEntryDTO> historyEntries(List<Order> orders) {
        // Chronological display, unknown timestamps last. UUIDs break ties deterministically,
        // but do not imply a known event chronology when timestamps are equal or missing.
        Comparator<OrderHistoryEntryDTO> chronological = Comparator
                .comparing(OrderHistoryEntryDTO::timestamp, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(entry -> entry.orderStatusChangeId() == null
                        ? "" : entry.orderStatusChangeId().toString());
        return orders.stream()
                .flatMap(order -> order.getOrderHistory().stream().map(change -> new OrderHistoryEntryDTO(
                        change.getOrderStatusChangeId(), order.getOrderId(), order.getClientId(),
                        change.getStatus(), change.getMessage(), change.getDate())))
                .sorted(chronological)
                .toList();
    }

    private Map<UUID, Instrument> loadInstruments(List<Order> orders) {
        Set<UUID> ids = orders.stream()
                .flatMap(order -> Stream.of(order.getAddInstrumentId(), order.getSubtractInstrumentId()))
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return instrumentRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Instrument::getID, Function.identity()));
    }

    private Map<UUID, List<Holding>> loadSettlements(List<Order> orders) {
        List<UUID> orderIds = orders.stream().map(Order::getOrderId).toList();
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return holdingRepository.findByOrderOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(holding -> holding.getOrder().getOrderId()));
    }

    private Map<UUID, String> loadClientNames(List<Order> orders) {
        Set<UUID> ids = orders.stream().map(Order::getClientId).filter(Objects::nonNull).collect(Collectors.toSet());
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(com.ceo.trading_platform_backend.models.User::getUserId,
                        com.ceo.trading_platform_backend.models.User::getFullName));
    }

    private AdminOrderResponseDTO toAdminResponse(Order order, Map<UUID, Instrument> instruments,
            Map<UUID, List<Holding>> settlements, Map<UUID, String> clientNames) {
        AdminSettlement values = AdminSettlement.from(order,
                settlements.getOrDefault(order.getOrderId(), List.of()), clientNames.get(order.getClientId()));
        Instrument added = order.getAddInstrumentId() == null ? null : instruments.get(order.getAddInstrumentId());
        Instrument subtracted = order.getSubtractInstrumentId() == null
                ? null : instruments.get(order.getSubtractInstrumentId());
        List<OrderStatusChange> history = order.getOrderHistory();
        Instant created = history.stream().map(OrderStatusChange::getDate)
                .filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
        OrderStatusChange latest = history.stream().max(HISTORY_ORDER).orElse(null);
        OrderStatus currentStatus = latest == null ? null : latest.getStatus();
        String resolved = currentStatus != null && RESOLVED_STATUSES.contains(currentStatus)
                && latest.getDate() != null ? latest.getDate().toString() : "";
        return new AdminOrderResponseDTO(order.getOrderId(), order.getClientId(), values.clientName(),
                order.getPortfolioId(), added == null ? null : added.getSymbol(),
                added == null ? null : added.getType(), added == null ? null : added.getFullName(),
                subtracted == null ? null : subtracted.getSymbol(), subtracted == null ? null : subtracted.getType(),
                subtracted == null ? null : subtracted.getFullName(), order.getQuantity(), order.getQuote(),
                values.addPurchasePrice(), values.subtractPurchasePrice(), values.unitsBought(), values.unitsSold(),
                values.cashBalanceDifference(), order.getSide(), order.getChangeThreshold(),
                created == null ? null : created.toString(), resolved, currentStatus);
    }

    private record AdminSettlement(String clientName, BigDecimal addPurchasePrice,
            BigDecimal subtractPurchasePrice, BigDecimal unitsBought, BigDecimal unitsSold,
            BigDecimal cashBalanceDifference) {
        static AdminSettlement from(Order order, List<Holding> holdings, String clientName) {
            BigDecimal addPrice = null;
            BigDecimal subtractPrice = null;
            BigDecimal bought = null;
            BigDecimal sold = null;
            BigDecimal cashDelta = null;
            for (Holding holding : holdings) {
                if (holding.getInstrument() == null) {
                    continue;
                }
                UUID instrumentId = holding.getInstrument().getID();
                if (instrumentId != null && instrumentId.equals(order.getAddInstrumentId())) {
                    addPrice = holding.getPurchasedPrice();
                }
                if (instrumentId != null && instrumentId.equals(order.getSubtractInstrumentId())) {
                    subtractPrice = holding.getPurchasedPrice();
                }
                if (holding.getInstrument().getType() == InstrumentType.CASH && holding.getQuantity() != null) {
                    cashDelta = (cashDelta == null ? BigDecimal.ZERO : cashDelta).add(holding.getQuantity());
                }
            }
            if (order.getQuantity() != null) {
                if (order.getSide() == com.ceo.trading_platform_backend.enums.Side.BUY) {
                    bought = order.getQuantity();
                } else if (order.getSide() == com.ceo.trading_platform_backend.enums.Side.SELL) {
                    sold = order.getQuantity();
                }
            }
            return new AdminSettlement(clientName, addPrice, subtractPrice, bought, sold, cashDelta);
        }
    }

    private OrderResponseDTO toResponse(Order order, Map<UUID, Instrument> instruments) {
        Instrument added = order.getAddInstrumentId() == null ? null : instruments.get(order.getAddInstrumentId());
        Instrument subtracted = order.getSubtractInstrumentId() == null
                ? null : instruments.get(order.getSubtractInstrumentId());
        List<OrderStatusChange> history = order.getOrderHistory();
        Instant created = history.stream().map(OrderStatusChange::getDate)
                .filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
        OrderStatusChange latest = history.stream().max(HISTORY_ORDER).orElse(null);
        OrderStatus status = latest == null ? null : latest.getStatus();
        String resolved = status != null && RESOLVED_STATUSES.contains(status) && latest.getDate() != null
                ? latest.getDate().toString() : "";

        // Missing history is unknown, not PENDING; do not fabricate timestamps for legacy records.
        return new OrderResponseDTO(order.getOrderId(), order.getClientId(), order.getPortfolioId(),
                added == null ? null : added.getSymbol(), added == null ? null : added.getType(),
                added == null ? null : added.getFullName(),
                subtracted == null ? null : subtracted.getSymbol(), subtracted == null ? null : subtracted.getType(),
                subtracted == null ? null : subtracted.getFullName(),
                order.getQuantity(), order.getQuote(), order.getSide(), order.getChangeThreshold(),
                created == null ? null : created.toString(), resolved, status);
    }

    private String normalize(String value, String parameter) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw badRequest(parameter + " must not be blank");
        }
        return value.strip();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}



