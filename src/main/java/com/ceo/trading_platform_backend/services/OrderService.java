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
        UUID addInstrumentId = instrumentService.getOrInsertInstrumentBySymbol(orderRequest.addInstrumentSymbol()).getID(); // instrument servcie asks external api for instrument info
        UUID subtractInstrumentId = instrumentService.getOrInsertInstrumentBySymbol(orderRequest.subtractInstrumentSymbol()).getID();
        BigDecimal quantity = orderRequest.quantity();
        BigDecimal quotedPrice = orderRequest.quotedPrice();
        Side side = orderRequest.side();
        BigDecimal changeThreshold = orderRequest.changeThreshold();
        Order order = new Order(addInstrumentId, subtractInstrumentId, side, portfolioId, clientId, quotedPrice, changeThreshold, quantity);
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

    public Order save(Order order) {
        return repository.save(order);
    }

    public void markRejected(UUID orderId, String message) {
        repository.findById(orderId).ifPresent(order -> {
            order.addOrderStatusChange(new OrderStatusChange(
                OrderStatus.REJECTED,
                message,
                Instant.now()
            ));
            repository.save(order);
        });
    }


    // these methods get called from kafka pipeline

    // validate info liek instrument tradable and has sufficient holdings
    public Order validate(Order order) {
        // TODO need to check time and put into different queues if doing after hours
        if (outOfHours(order, order.getCreatedDate())) {
            Instant today = Instant.now();
            OrderStatusChange rejectedOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "market closed, rejecting", today); // case in which subitted before hours, after validation is after
            order.addOrderStatusChange(rejectedOrderStatus);
            return order; // early return, don't validate --> this functionality may change depending on if allowing extra hours
        }
        if (!instrumentIsTradeable(order.getAddInstrumentId())) {
            Instant today = Instant.now();
            String symbol = instrumentService.getInstrumentById(order.getAddInstrumentId()).getSymbol();
            OrderStatusChange rejectedOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "instrument" + symbol + "not tradable", today);
            order.addOrderStatusChange(rejectedOrderStatus);
            return order;
        } else if (!instrumentIsTradeable(order.getSubtractInstrumentId())){
            Instant today = Instant.now();
            String symbol = instrumentService.getInstrumentById(order.getSubtractInstrumentId()).getSymbol();
            OrderStatusChange rejectedOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "instrument" + symbol + "not tradable", today);
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

    public Order execute(Order order) {
        BigDecimal addPrice = marketService.getPrice(instrumentService.getInstrumentById(order.getAddInstrumentId()).getSymbol());
        BigDecimal subtractPrice = marketService.getPrice(instrumentService.getInstrumentById(order.getSubtractInstrumentId()).getSymbol());
        BigDecimal activePrice = (order.getSide() == Side.SELL) ? subtractPrice : addPrice;

        if (outOfHours(order, Instant.now())) {
            Instant today = Instant.now();
            OrderStatusChange rejectedOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "market closed, rejecting", today); // case in which subitted before hours, after validation is after
            order.addOrderStatusChange(rejectedOrderStatus);
            return order; // early return, don't execute
        }
        if (!withinChangeThreshold(order, activePrice)) {
            Instant today = Instant.now();
            OrderStatusChange rejectedOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "price " + activePrice + " changed past threshold since submitting "+ order.getQuote(), today);
            order.addOrderStatusChange(rejectedOrderStatus);
            return order;
        }
        if (order.getSide() == Side.BUY) {
            if (!hasSufficientFundsBuy(order, activePrice)) {
                Instant today = Instant.now();
                OrderStatusChange rejectedOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "insufficient funds", today);
                order.addOrderStatusChange(rejectedOrderStatus);
                return order;
            }
        }
  
        portfolioService.updateHoldingsFromOrder(order, addPrice, subtractPrice);
        // update status - shoudl this happen based on portfolio service results above or is it just assumed thos will work?
        Instant today = Instant.now();
        OrderStatusChange fulfilledOrderStatus = new OrderStatusChange(OrderStatus.FULFILLED, "order fulfilled", today);
        order.addOrderStatusChange(fulfilledOrderStatus);
        return order;
    }

    // get all orders of a portfolio
    public List<Order> getOrdersByPortfolioId(UUID ID) { // currently int but shoudl not be...
        List<Order> orders = this.repository.findByPortfolioId(ID);
        return orders;
    } 

    // check if given time stamp is out of hours of market (createdDate for validaiton, now for execution)
    private boolean outOfHours(Order order, Instant timestamp) {
        ZoneId zone = ZoneId.of("America/New_York");
        LocalDateTime dateTime = LocalDateTime.ofInstant(timestamp, zone);
        LocalTime time = dateTime.toLocalTime();
        DayOfWeek day = dateTime.toLocalDate().getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return true;
        }
        if (time.isBefore(OPEN_TIME_LOCAL) || time.isAfter(CLOSE_TIME_LOCAL)) { // submitted time shoudl be in request dto
            return true;
        }
        return false;
    }

    // check if the price has changed too much since ordering
    private boolean withinChangeThreshold(Order order, BigDecimal activePrice) {
        BigDecimal changeThreshold = order.getChangeThreshold();
        BigDecimal quotedPrice = order.getQuote();
        BigDecimal toleratedPrice;
        if (order.getSide() == Side.BUY) {
            toleratedPrice = quotedPrice.add(quotedPrice.multiply(changeThreshold));
            if (activePrice.compareTo(toleratedPrice) > 0) {
                return false;
            }
        } else {
            toleratedPrice = quotedPrice.subtract(quotedPrice.multiply(changeThreshold));
            if (activePrice.compareTo(toleratedPrice) < 0)  {
                return false;
            }
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
            if (holding.getInstrument().equals(instrumentService.getInstrumentById(order.getSubtractInstrumentId()))) { // selling instrument is subtract
                numSharesOwned = numSharesOwned.add(holding.getQuantity());
            }
        }

        if (numSharesOwned.compareTo(order.getQuantity()) < 0) {
            return false;
        }
        
        return true;
    }

    private boolean instrumentIsTradeable(UUID instrumentId) {
        // comes from market API
        Boolean tradable = marketService.isActiveSymbol(instrumentService.getInstrumentById(instrumentId).getSymbol());
        return tradable;
    }
}
