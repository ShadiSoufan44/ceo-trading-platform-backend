package com.ceo.trading_platform_backend.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.math.BigDecimal;

import com.ceo.trading_platform_backend.uml_objects.Client;
import com.ceo.trading_platform_backend.uml_objects.Holding;
import com.ceo.trading_platform_backend.uml_objects.Instrument;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.uml_objects.OrderStatusChange;
import com.ceo.trading_platform_backend.uml_objects.Enums.InstrumentType;
import com.ceo.trading_platform_backend.uml_objects.Enums.OrderStatus;
import com.ceo.trading_platform_backend.uml_objects.Enums.Side;

import jakarta.annotation.Resource;

import com.ceo.trading_platform_backend.repositories.OrderRepository; // waiting for implementation
import com.ceo.trading_platform_backend.dto.OrderRequestDTO;
import com.ceo.trading_platform_backend.dto.OrderResponseDTO;

/*
 * 
 * OrderService
 * TODO:
 * submit
 * validate
 * execute
 * execute after hours?
 * 
 * use the repo (jpa stuff) functions to get info about the order
 * 
 */

public class OrderService {
    

    //service object to communicate with API
    private final OrderRepository repository;
    private final ClientService clientService;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public Order createOrder(OrderRequestDTO orderRequest) {
        int clientId = orderRequest.clientId();
        int portfolioId = portfolioService.getPortfolioById(orderRequest.portfolioId());
        Integer instrumentId = instrumentService.getOrInsertInstrumentBySymbol(orderRequest.instrumentSymbol()).getId(); // instrument servcie asks external api for instrument info
        Date createdDate = new Date();
        BigDecimal quantity = orderRequest.quantity();
        BigDecimal quotedPrice = orderRequest.quotedPrice();
        Side side = orderRequest.side();
        BigDecimal increaseThreshold = orderRequest.increaseThreshold();
        Order order = new Order(instrumentId, side, portfolioId, clientId, quotedPrice, increaseThreshold, quantity);
        this.repository.save(order);
        return order;
    }

    // public Order submit(OrderRequestDTO orderRequest) {
    //     Order order = createOrder(orderRequest);
    //     Date submittedTime = new Date(); // add time filed so we know what tie order was subitted fo rafter hour processing
    //     return validate(order, submittedTime);
    // }

    // current plan of aciton
    // validation --> checks whatever it can
    // send to queue
    // validation + execute --> check liek info liek price and execute

    // these methods get claled from kafka pipeline

    // validate info liek instrument tradable and has sufficient holdings
    private Order validate(Order order) {
        // TODO need to check time and put into different queues if doing after hours
        if (!instrumentIsTradeable(order)) {
            Date today = new Date();
            OrderStatusChange rejectedOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "instrument not tradable", today);
            order.addOrderStatusChange(rejectedOrderStatus);
            return order;
        }
        if (order.getSide() == Side.SELL) {
            if (!hasSufficientHoldingsSell(order)) {
                Date today = new Date();
                OrderStatusChange rejectedOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "insufficient holdings", today);
                order.addOrderStatusChange(rejectedOrderStatus);
                return order;
            }
        }
        Date today = new Date();
        OrderStatusChange acceptedOrderStatus = new OrderStatusChange(OrderStatus.ACCEPTED, "order accepted", today);
        order.addOrderStatusChange(acceptedOrderStatus);
        return order; // go ahead to send to queue base don order status
    }

    private Order execute(Order order) {
        if (order.getSide() == Side.BUY) {
            if (!hasSufficientFundsBuy(order)) {
                Date today = new Date();
                OrderStatusChange rejectedOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "insufficient funds", today);
                order.addOrderStatusChange(rejectedOrderStatus);
                return order;
            }
        }
        Date marketOpen = new Date(); // general times...?
        Date marketClose = new Date();
        Date submittedTime = order.getCreatedDate();
        if (submittedTime < marketOpen || submittedTime > marketClose) { // submitted time shoudl be in request dto
            Date today = new Date();
            OrderStatusChange pendingOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "market closed, rejecting", today); // case in which subitted before hours, after validation is after
            order.addOrderStatusChange(pendingOrderStatus);
            return order; // early return, don't execute
        }
        portfolioService.updateHoldingsFromOrder(order);
        // update status - shoudl this happen based on portfolio service results above or is it just assumed thos will work?
        Date today = new Date();
        OrderStatusChange fulfilledOrderStatus = new OrderStatusChange(OrderStatus.FUFILLED, "order fulfilled", today);
        order.addOrderStatusChange(fulfilledOrderStatus);
        return order;
    }


    // TODO: fix placeholders like date and stuff
    public OrderResponseDTO createOrderResponse(Order order) {
        // controller gets response back
        Integer orderId = order.getOrderId();
        Integer clientId = order.getUserId();
        Integer portfolioId = order.getPortfolioId();
        String instrumentSymbol = instrumentService.getInstrumentById(order.getInstrumentId()).getInstrumentSymbol();
        String instrumentFullName = instrumentService.getInstrumentById(order.getInstrumentId()).getInstrumentfullName();
        InstrumentType instrumentType = instrumentService.getInstrumentById(order.getInstrumentId()).getInstrumentType();
        String createdDate = order.getCreatedDate().toString();
        BigDecimal quantity = order.getQuantity();
        BigDecimal quotedPrice = order.getQuote();
        Side side = order.getSide();
        BigDecimal increaseThreshold = order.getIncreaseThreshold();
        String resolvedDate = ""; // not resolved yet?
        OrderStatus currentStatus = order.getCurrentOrderStatus().getStatus();
        OrderResponseDTO response = new OrderResponseDTO(orderId, clientId, portfolioId, instrumentSymbol, instrumentType, instrumentFullName, quantity, quotedPrice, side, increaseThreshold, createdDate, resolvedDate, currentStatus); 
    }

    // get orders by portfolio
    // portfolioService calls orderRepository
    //
    // portfolioService -> orderService -> orderRepository
    // portfolioService -> portfolioRepository and it owudl do joins
    // method for portfolios service to use to find which orders belong to it
    // do we need this method or woudl it just be holdings? but i think we should still need it because we woudl want transaction history to reflect based on chosen portfolio right?
    // does history belong to portfolio...?
    public Order getOrderByPortfolioId(int ID) { // currently int but shoudl not be...
        this.repository.findByPortfolioId(ID).map(OrderMapper::toDto).orElseThrow(() -> new ResourceNortFoundException("order not found"));
    }


    // method for processing after hours orders
    // not requested by front end
    // chganging now that using queueue...?
    // public List<Order> processBatchOrders() {
    //     List<Order> afterHourOrders = new ArrayList<Order>(); // or whatever repository method returns
    //     afterHoursOrders = this.repository.findByStatus(OrderStatus.PENDING); //jpa knows its order table because its order repo
    //     List<Order> processedAfterHoursOrders = new ArrayList<Order>();
    //     for (Order order : afterHourOrders) {
    //         Order processedOrder = this.validate(order); // process (val + exec) instead, since validate shouldnt call eexecute anymore
    //         processedAfterHoursOrders.add(processedOrder);
    //     }
    //     return processedAfterHoursOrders;
    // }

    // Buy Order
    private static boolean hasSufficientFundsBuy(Order order) throws Exception {
        double funds = portfolioService.getPortfolioById(order.getPortfolioId()).getUSDCash();
        double cost = order.getQuantity() * order.getQuote(); // should be evaluating against real price from marketService
        return funds >= cost;
    }

    // Sell Order

    // 
    private static boolean hasSufficientHoldingsSell(Order order) {
        List<Holding> holdings = order.portfolioService.getPortfolioById(order.getPortfolioID()).getHoldings();

        double numSharesOwned = 0;

        for (Holding holding : holdings) {
            if (holding.getInstrument().equals(order.getInstrument())) {
                numSharesOwned += holding.getQuantity();
            }
        }

        if (numSharesOwned < order.getQuantity()) {
            return false;
        }
        
        return true;
    }

    private static boolean instrumentIsTradeable(Order order) {
        // comes from market API
        return true;
    }




}
