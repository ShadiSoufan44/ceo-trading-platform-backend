package com.ceo.trading_platform_backend.services;

// import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.uml_objects.OrderStatusChange;
import com.ceo.trading_platform_backend.uml_objects.Enums.OrderStatus;
import com.ceo.trading_platform_backend.uml_objects.Enums.Side;

import jakarta.annotation.Resource;

import com.ceo.trading_platform_backend.repositories.OrderRepository; // waiting for implementation
import com.ceo.trading_platform_backend.dto.OrderRequestDTO;

@Service
public class OrderService {
    

    //service object to communicate with API
    private final OrderRepository repository;
    private final PortfolioService portfolioService;
    private final InstrumentService instrumentService;
    private final MarketService marketService;

    public OrderService(OrderRepository repository, PortfolioService portfolioService, InstrumentService instrumentService, MarketService marketService) {
        this.repository = repository;
        this.portfolioService = portfolioService;
        this.instrumentService = instrumentService;
        this.marketService = marketService;
    }

    // TODO move to ordercontroller and orderservice only gets order objects?
    public Order createOrder(OrderRequestDTO orderRequest) {
        int clientId = orderRequest.clientId();
        int portfolioId = portfolioService.getPortfolioById(orderRequest.portfolioId()).getPortfolioId();
        Integer instrumentId = instrumentService.getOrInsertInstrumentBySymbol(orderRequest.instrumentSymbol()).getID(); // instrument servcie asks external api for instrument info
        BigDecimal quantity = orderRequest.quantity();
        BigDecimal quotedPrice = orderRequest.quotedPrice();
        Side side = orderRequest.side();
        BigDecimal increaseThreshold = orderRequest.increaseThreshold();
        Order order = new Order(instrumentId, side, portfolioId, clientId, quotedPrice, increaseThreshold, quantity);
        this.repository.save(order);
        return order;
    }

    public List<Order> getAllOrders() {
        List<Order> allOrders = this.repository.findAll();
        return allOrders;
    }

    public List<Order> getClientOrders(int clientId) {
        List<Order> orders = this.repository.findByClientId(clientId);
        return orders;
    }

    public Optional<Order> getOrderById(int orderId) {
        Optional<Order> order = this.repository.findById(orderId); // shoudl throw error if not found?
        return order;
    }

    // these methods get called from kafka pipeline

    // validate info liek instrument tradable and has sufficient holdings
    private Order validate(Order order) {
        // TODO need to check time and put into different queues if doing after hours
        if (!instrumentIsTradeable(order)) {
            LocalDateTime today = LocalDateTime.now();
            OrderStatusChange rejectedOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "instrument not tradable", today);
            order.addOrderStatusChange(rejectedOrderStatus);
            return order;
        }
        if (order.getSide() == Side.SELL) {
            if (!hasSufficientHoldingsSell(order)) {
                LocalDateTime today = LocalDateTime.now();
                OrderStatusChange rejectedOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "insufficient holdings", today);
                order.addOrderStatusChange(rejectedOrderStatus);
                return order;
            }
        }
        LocalDateTime today = LocalDateTime.now();
        OrderStatusChange acceptedOrderStatus = new OrderStatusChange(OrderStatus.ACCEPTED, "order accepted", today);
        order.addOrderStatusChange(acceptedOrderStatus);
        return order; // go ahead to send to queue base don order status
    }

    private Order execute(Order order) {
        BigDecimal price =  marketService.getPrice(instrumentService.getInstrumentById(order.getInstrumentId()).getSymbol());
        if (outOfHours(order)) {
            LocalDateTime today = LocalDateTime.now();
            OrderStatusChange pendingOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "market closed, rejecting", today); // case in which subitted before hours, after validation is after
            order.addOrderStatusChange(pendingOrderStatus);
            return order; // early return, don't execute
        }
        if (order.getSide() == Side.BUY) {
            if (!hasSufficientFundsBuy(order, price)) {
                LocalDateTime today = LocalDateTime.now();
                OrderStatusChange rejectedOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "insufficient funds", today);
                order.addOrderStatusChange(rejectedOrderStatus);
                return order;
            }
        }
  
        portfolioService.updateHoldingsFromOrder(order, price);
        // update status - shoudl this happen based on portfolio service results above or is it just assumed thos will work?
        LocalDateTime today = LocalDateTime.now();
        OrderStatusChange fulfilledOrderStatus = new OrderStatusChange(OrderStatus.FUFILLED, "order fulfilled", today);
        order.addOrderStatusChange(fulfilledOrderStatus);
        return order;
    }


    // TODO: fix placeholders like date and stuff
    // public OrderResponseDTO createOrderResponse(Order order) {
    //     // controller gets response back
    //     Integer orderId = order.getOrderId();
    //     Integer clientId = order.getUserId();
    //     Integer portfolioId = order.getPortfolioId();
    //     String instrumentSymbol = instrumentService.getInstrumentById(order.getInstrumentId()).getSymbol();
    //     String instrumentFullName = instrumentService.getInstrumentById(order.getInstrumentId()).getFullName();
    //     InstrumentType instrumentType = instrumentService.getInstrumentById(order.getInstrumentId()).getType();
    //     String createdDate = order.getCreatedDate().toString();
    //     BigDecimal quantity = order.getQuantity();
    //     BigDecimal quotedPrice = order.getQuote();
    //     Side side = order.getSide();
    //     BigDecimal increaseThreshold = order.getIncreaseThreshold();
    //     String resolvedDate = ""; // not resolved yet?
    //     OrderStatus currentStatus = order.getCurrentOrderStatus().getStatus();
    //     OrderResponseDTO response = new OrderResponseDTO(orderId, clientId, portfolioId, instrumentSymbol, instrumentType, instrumentFullName, quantity, quotedPrice, side, increaseThreshold, createdDate, resolvedDate, currentStatus); 
        
    //     return response;
    // }

    // get all orders of a portfolio
    public List<Order> getOrdersByPortfolioId(int ID) { // currently int but shoudl not be...
        List<Order> orders = this.repository.findByPortfolioId(ID);
        return orders;
    } 


    // Check if in hours
    private boolean outOfHours(Order order) {
        // TODO fix time
        // use LocalTime to capture just time for market open and close
        // use instant for the order submissions time
        // convert this instant to zoned then to the local time (.toLocalTime()) of the market
        // compare in the ny time zone
        // also check day of week from the zoneddatetiem
        LocalDate currentDay = LocalDate.now();
        LocalDate nextDay = currentDay.plusDays(1);
        LocalTime openTime = LocalTime.of(9, 30, 0);
        LocalTime closeTime = LocalTime.of(16, 0, 0);
        LocalDateTime marketOpen = LocalDateTime.of(nextDay, openTime); // general times...?
        LocalDateTime marketClose = LocalDateTime.of(currentDay, closeTime);
        LocalDateTime submittedTime = order.getCreatedDate();
        if (submittedTime.isBefore(marketOpen) || submittedTime.isAfter(marketClose)) { // submitted time shoudl be in request dto
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
