package com.ceo.trading_platform_backend.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.enums.Side;
import com.ceo.trading_platform_backend.exception.ResourceNotFoundException;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.OrderStatusChange;
import com.ceo.trading_platform_backend.repositories.InstrumentRepository;
import com.ceo.trading_platform_backend.repositories.HoldingRepository;
import com.ceo.trading_platform_backend.repositories.OrderRepository;
import com.ceo.trading_platform_backend.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTests {
    @Mock private OrderRepository orderRepository;
    @Mock private InstrumentRepository instrumentRepository;
    @Mock private HoldingRepository holdingRepository;
    @Mock private UserRepository userRepository;
    private AdminService service;

    @BeforeEach
    void setUp() {
        service = new AdminService(orderRepository, instrumentRepository, holdingRepository, userRepository);
    }

    @Test
    void noFiltersQueriesAllOrdersWithoutDateOrStatusRestriction() {
        var pageable = PageRequest.of(0, 20);
        when(orderRepository.searchAdminOrders(null, null, null, null, null, null, pageable))
                .thenReturn(Page.empty(pageable));

        assertThat(service.getOrders(null, null, null, null, null, null, 0, 20)).isEmpty();
        verify(orderRepository).searchAdminOrders(null, null, null, null, null, null, pageable);
        verifyNoInteractions(instrumentRepository);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void combinesFiltersNormalizesTextAndUsesInclusiveUtcDates() {
        UUID userId = UUID.randomUUID();
        var pageable = PageRequest.of(2, 10);
        Instant from = Instant.parse("2026-10-01T00:00:00Z");
        Instant end = Instant.parse("2026-10-27T00:00:00Z");
        when(orderRepository.searchAdminOrders(userId, "%jo!_!%!!%", "FULFILLED", from, end, "AAPL", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 15));

        var result = service.getOrders(userId, " Jo_%! ", OrderStatus.FULFILLED,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 26), " aapl ", 2, 10);

        assertThat(result.getTotalElements()).isEqualTo(15);
        verify(orderRepository).searchAdminOrders(userId, "%jo!_!%!!%", "FULFILLED", from, end, "AAPL", pageable);
    }

    @Test
    void supportsOneSidedDateBounds() {
        when(orderRepository.searchAdminOrders(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        LocalDate day = LocalDate.of(2026, 10, 26);

        service.getOrders(null, null, null, day, null, null, 0, 20);
        service.getOrders(null, null, null, null, day, null, 0, 20);

        verify(orderRepository).searchAdminOrders(null, null, null,
                Instant.parse("2026-10-26T00:00:00Z"), null, null, PageRequest.of(0, 20));
        verify(orderRepository).searchAdminOrders(null, null, null,
                null, Instant.parse("2026-10-27T00:00:00Z"), null, PageRequest.of(0, 20));
    }

    @Test
    void validatesInputsBeforeQuerying() {
        assertBadRequest(() -> service.getOrders(null, null, null, null, null, null, -1, 20));
        assertBadRequest(() -> service.getOrders(null, null, null, null, null, null, 0, 0));
        assertBadRequest(() -> service.getOrders(null, null, null, null, null, null, 0, 101));
        assertBadRequest(() -> service.getOrders(null, null, null, null, null, null, Integer.MAX_VALUE, 20));
        assertBadRequest(() -> service.getOrders(null, " ", null, null, null, null, 0, 20));
        assertBadRequest(() -> service.getOrders(null, null, null, null, null, "\t", 0, 20));
        assertBadRequest(() -> service.getOrders(null, null, null,
                LocalDate.of(2026, 10, 27), LocalDate.of(2026, 10, 26), null, 0, 20));
        assertBadRequest(() -> service.getOrders(null, null, null, null, LocalDate.MAX, null, 0, 20));
        verifyNoInteractions(orderRepository, instrumentRepository);
    }

    @Test
    void lookupUsesChronologicalHistoryAndHandlesDepositWithNoSubtractInstrument() {
        UUID instrumentId = UUID.randomUUID();
        Order order = order(instrumentId);
        // Deliberately reversed collection order, as an unordered JPA collection may be loaded.
        order.addOrderStatusChange(change(OrderStatus.FULFILLED, "2026-10-26T12:00:00Z", "80000000"));
        order.addOrderStatusChange(change(OrderStatus.PENDING, "2026-10-25T12:00:00Z", "00000000"));
        when(orderRepository.findWithHistoryByOrderIdIn(List.of(order.getOrderId()))).thenReturn(List.of(order));
        Instrument usd = new Instrument("USD", InstrumentType.CASH, "United States Dollar");
        ReflectionTestUtils.setField(usd, "ID", instrumentId);
        when(instrumentRepository.findAllById(Set.of(instrumentId))).thenReturn(List.of(usd));

        var result = service.getOrderById(order.getOrderId());

        assertThat(result.currentStatus()).isEqualTo(OrderStatus.FULFILLED);
        assertThat(result.createdDate()).isEqualTo("2026-10-25T12:00:00Z");
        assertThat(result.resolvedDate()).isEqualTo("2026-10-26T12:00:00Z");
        assertThat(result.addInstrumentSymbol()).isEqualTo("USD");
        assertThat(result.subtractInstrumentSymbol()).isNull();
        verify(instrumentRepository).findAllById(Set.of(instrumentId));
    }

    @Test
    void equalTimestampsUsePostgresCompatibleUuidTieBreaker() {
        Order order = order(null);
        order.addOrderStatusChange(change(OrderStatus.CANCELLED, "2026-10-26T12:00:00Z", "80000000"));
        order.addOrderStatusChange(change(OrderStatus.ACCEPTED, "2026-10-26T12:00:00Z", "00000000"));
        when(orderRepository.findWithHistoryByOrderIdIn(List.of(order.getOrderId()))).thenReturn(List.of(order));

        assertThat(service.getOrderById(order.getOrderId()).currentStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void legacyOrdersWithoutHistoryOrInstrumentsRemainVisible() {
        Order order = order(null);
        when(orderRepository.findWithHistoryByOrderIdIn(List.of(order.getOrderId()))).thenReturn(List.of(order));

        var result = service.getOrderById(order.getOrderId());

        assertThat(result.orderId()).isEqualTo(order.getOrderId());
        assertThat(result.currentStatus()).isNull();
        assertThat(result.createdDate()).isNull();
        assertThat(result.resolvedDate()).isEmpty();
        verifyNoInteractions(instrumentRepository);
    }

    @Test
    void undatedHistoryDoesNotOverrideDatedHistory() {
        Order order = order(null);
        order.addOrderStatusChange(change(OrderStatus.ACCEPTED, "2026-10-26T12:00:00Z", "00000000"));
        order.addOrderStatusChange(new OrderStatusChange(OrderStatus.REJECTED, "undated", null));
        when(orderRepository.findWithHistoryByOrderIdIn(List.of(order.getOrderId()))).thenReturn(List.of(order));

        var result = service.getOrderById(order.getOrderId());

        assertThat(result.currentStatus()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(result.createdDate()).isEqualTo("2026-10-26T12:00:00Z");
        assertThat(result.resolvedDate()).isEmpty();
    }

    @Test
    void unknownOrderThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findWithHistoryByOrderIdIn(List.of(id))).thenReturn(List.of());
        assertThatThrownBy(() -> service.getOrderById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void mappingPreservesDatabasePageOrderAndTotals() {
        Order first = order(null);
        Order second = order(null);
        var pageable = PageRequest.of(0, 2);
        when(orderRepository.searchAdminOrders(null, null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(first, second), pageable, 12));
        when(orderRepository.findWithHistoryByOrderIdIn(List.of(first.getOrderId(), second.getOrderId())))
                .thenReturn(List.of(second, first));
        when(holdingRepository.findByOrderOrderIdIn(anyList())).thenReturn(List.of());
        when(userRepository.findAllById(anySet())).thenReturn(List.of());

        var result = service.getOrders(null, null, null, null, null, null, 0, 2);

        assertThat(result.getTotalElements()).isEqualTo(12);
        assertThat(result.getContent()).extracting(dto -> dto.orderId())
                .containsExactly(first.getOrderId(), second.getOrderId());
    }

    private Order order(UUID addedInstrumentId) {
        Order order = new Order(addedInstrumentId, null, Side.DEPOSIT, UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN, "America/New_York");
        ReflectionTestUtils.setField(order, "orderId", UUID.randomUUID());
        return order;
    }

    private OrderStatusChange change(OrderStatus status, String time, String idPrefix) {
        var change = new OrderStatusChange(status, "test event", Instant.parse(time));
        ReflectionTestUtils.setField(change, "orderStatusChangeId",
                UUID.fromString(idPrefix + "-0000-0000-0000-000000000001"));
        return change;
    }

    private void assertBadRequest(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}