package com.ceo.trading_platform_backend.services;

// import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
// import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.DayOfWeek;
import java.time.Instant;

import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.OrderStatusChange;

import com.ceo.trading_platform_backend.repositories.OrderRepository; // waiting for implementation
import com.ceo.trading_platform_backend.dto.OrderRequestDTO;
import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.enums.Side;

@Service
public class OrderService {
    

    //service object to communicate with API
    private final OrderRepository repository;
    private final PortfolioService portfolioService;
    private final InstrumentService instrumentService;
    private final MarketService marketService;

    private final LocalTime OPEN_TIME_LOCAL = LocalTime.of(9, 30, 0); // shoudl these be find
    private final LocalTime CLOSE_TIME_LOCAL = LocalTime.of(16, 0, 0);

    public OrderService(OrderRepository repository, PortfolioService portfolioService, InstrumentService instrumentService, MarketService marketService) {
        this.repository = repository;
        this.portfolioService = portfolioService;
        this.instrumentService = instrumentService;
        this.marketService = marketService;
    }

    // TODO move to ordercontroller and orderservice only gets order objects?
    public Order createOrder(OrderRequestDTO orderRequest) {
        UUID clientId = orderRequest.clientId();
        UUID portfolioId = portfolioService.getPortfolioById(orderRequest.portfolioId()).getPortfolioId();
        UUID instrumentId = instrumentService.getOrInsertInstrumentBySymbol(orderRequest.instrumentSymbol()).getID(); // instrument servcie asks external api for instrument info
        BigDecimal quantity = orderRequest.quantity();
        BigDecimal quotedPrice = orderRequest.quotedPrice();
        Side side = orderRequest.side();
        BigDecimal increaseThreshold = orderRequest.increaseThreshold();
        Order order = new Order(instrumentId, side, portfolioId, clientId, quotedPrice, increaseThreshold, quantity);
        order = this.repository.save(order);
        return order;
    }

    public List<Order> getAllOrders() {
        List<Order> allOrders = this.repository.findAll();
        return allOrders;
    }

    public List<Order> getClientOrders(UUID clientId) {
        List<Order> orders = this.repository.findByClientId(clientId);
        return orders;
    }

    public Optional<Order> getOrderById(UUID orderId) {
        Optional<Order> order = this.repository.findById(orderId); // shoudl throw error if not found?
        return order;
    }

    public Order submit(OrderRequestDTO orderRequest) {
        Order order = createOrder(orderRequest);
        // set pending
        Instant today = Instant.now();
        OrderStatusChange pendingOrderStatus = new OrderStatusChange(OrderStatus.PENDING, "order submitted to be processed", today);
        order.addOrderStatusChange(pendingOrderStatus);
        repository.save(order);
        
        Order validatedOrder = validate(order);
        if (validatedOrder.getCurrentOrderStatus().getStatus() == OrderStatus.ACCEPTED) {
            return execute(validatedOrder);
        } 
        return repository.save(validatedOrder);
    }

    // these methods get called from kafka pipeline

    // validate info liek instrument tradable and has sufficient holdings
    private Order validate(Order order) {
        // TODO need to check time and put into different queues if doing after hours
        if (!instrumentIsTradeable(order)) {
            Instant today = Instant.now();
            OrderStatusChange rejectedOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "instrument not tradable", today);
            order.addOrderStatusChange(rejectedOrderStatus);
            return order;
        }
        if (order.getSide() == Side.SELL) {
            if (!hasSufficientHoldingsSell(order)) {
                Instant today = Instant.now();
                OrderStatusChange rejectedOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "insufficient holdings", today);
                order.addOrderStatusChange(rejectedOrderStatus);
                return order;
            }
        }
        Instant today = Instant.now();
        OrderStatusChange acceptedOrderStatus = new OrderStatusChange(OrderStatus.ACCEPTED, "order accepted", today);
        order.addOrderStatusChange(acceptedOrderStatus);
        return order; // go ahead to send to queue base don order status
    }

    private Order execute(Order order) {
        BigDecimal price =  marketService.getPrice(instrumentService.getInstrumentById(order.getInstrumentId()).getSymbol());
        if (outOfHours(order)) {
            Instant today = Instant.now();
            OrderStatusChange pendingOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "market closed, rejecting", today); // case in which subitted before hours, after validation is after
            order.addOrderStatusChange(pendingOrderStatus);
            return order; // early return, don't execute
        }
        if (!withinIncreaseThreshold(order, price)) {
            Instant today = Instant.now();
            OrderStatusChange rejectedOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "price " + price + " increased past threshold since submitting "+ order.getQuote(), today);
            order.addOrderStatusChange(rejectedOrderStatus);
            return order;
        }
        if (order.getSide() == Side.BUY) {
            if (!hasSufficientFundsBuy(order, price)) {
                Instant today = Instant.now();
                OrderStatusChange rejectedOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "insufficient funds", today);
                order.addOrderStatusChange(rejectedOrderStatus);
                return order;
            }
        }
  
        portfolioService.updateHoldingsFromOrder(order, price);
        // update status - shoudl this happen based on portfolio service results above or is it just assumed thos will work?
        Instant today = Instant.now();
        OrderStatusChange fulfilledOrderStatus = new OrderStatusChange(OrderStatus.FUFILLED, "order fulfilled", today);
        order.addOrderStatusChange(fulfilledOrderStatus);
        return order;
    }

    // get all orders of a portfolio
    public List<Order> getOrdersByPortfolioId(UUID ID) { // currently int but shoudl not be...
        List<Order> orders = this.repository.findByPortfolioId(ID);
        return orders;
    } 

    // Check if in hours
    private boolean outOfHours(Order order) {
        Instant submittedInstant = order.getCreatedDate();
        ZoneId zone = ZoneId.of("America/New_York");
        LocalDateTime submittedDateTime = LocalDateTime.ofInstant(submittedInstant, zone);
        LocalTime submittedTime = submittedDateTime.toLocalTime();
        DayOfWeek submittedDay = submittedDateTime.toLocalDate().getDayOfWeek();
        if (submittedDay == DayOfWeek.SATURDAY || submittedDay == DayOfWeek.SUNDAY) {
            return true;
        }
        if (submittedTime.isBefore(OPEN_TIME_LOCAL) || submittedTime.isAfter(CLOSE_TIME_LOCAL)) { // submitted time shoudl be in request dto
            return true;
        }
        return false;
    }

    // check if the price has increased too much since ordering
    private boolean withinIncreaseThreshold(Order order, BigDecimal price) {
        BigDecimal increaseThreshold = order.getIncreaseThreshold();
        BigDecimal quotedPrice = order.getQuote();
        BigDecimal toleratedPrice = quotedPrice.add(quotedPrice.multiply(increaseThreshold));
        if (price.compareTo(toleratedPrice) > 0) {
            return false;
        }
        return true;
    }

    // Buy Order
    private boolean hasSufficientFundsBuy(Order order, BigDecimal price){
        BigDecimal funds = portfolioService.getBuyingPowerByPortfolioId(order.getPortfolioId());
        BigDecimal cost = order.getQuantity().multiply(price); // should be evaluating against real price from marketService
        return funds.compareTo(cost) >= 0;
    }
    
    private boolean hasSufficientHoldingsSell(Order order) {
        List<Holding> holdings = portfolioService.getHoldingsByPortfolioId(order.getPortfolioId());

        BigDecimal numSharesOwned = BigDecimal.ZERO;

        for (Holding holding : holdings) {
            if (holding.getInstrument().equals(instrumentService.getInstrumentById(order.getInstrumentId()))) {
                numSharesOwned = numSharesOwned.add(holding.getQuantity());
            }
        }

        if (numSharesOwned.compareTo(order.getQuantity()) < 0) {
            return false;
        }
        
        return true;
    }

    private boolean instrumentIsTradeable(Order order) {
        // comes from market API
        Boolean tradable = marketService.isActiveSymbol(instrumentService.getInstrumentById(order.getInstrumentId()).getSymbol());
        return tradable;
    }
}
