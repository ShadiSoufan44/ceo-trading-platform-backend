package com.ceo.trading_platform_backend.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ceo.trading_platform_backend.uml_objects.Client;
import com.ceo.trading_platform_backend.uml_objects.Holding;
import com.ceo.trading_platform_backend.uml_objects.Instrument;
import com.ceo.trading_platform_backend.uml_objects.Order;
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
        Client client = clientService.getUserById(orderRequest.clientId());
        int portfolioId = portfolioService.getPortfolioById(orderRequest.portfolioId());
        Instrument instrument = instrumentService.getInstrumentById(orderRequest.instrumentSymbol()); // instrument servcie asks external api for instrument info
        Date createdDate = new Date();
        double quantity = orderRequest.quantity();
        double quotedPrice = orderRequest.quotedPrice();
        Side side = orderRequest.side();
        double increaseThreshold = orderRequest.increaseThreshold();
        Order order = new Order(client, instrument, portfolioId, createdDate, quantity, quotedPrice, increaseThreshold);
        OrderStatusChange pendingOrderStatus = new OrderStatusChange(OrderStatus.PENDING, "order created", today);
        order.addOrderStatusChange(pendingOrderStatus);
        this.repository.save(order);
        return order;
    }

    public Order submit(OrderRequestDTO orderRequest) {
        Order order = createOrder(orderRequest);
        Date submittedTime = new Date(); // add time filed so we know what tie order was subitted fo rafter hour processing
        return validate(order, submittedTime);
    }

    private Order validate(Order order, Date submittedTime) { // what is the flow? make less redundant?
        // NEED TO CHECK TIME? if after horus then leave pending...
        Date marketOpen = new Date(); // general times...?
        Date marketClose = new Date();
        if (submittedTime < marketOpen || submittedTime > marketClose) {
            Date today = new Date();
            OrderStatusChange pendingOrderStatus = new OrderStatusChange(OrderStatus.PENDING, "market closed, will execute on market open", today);
            order.addOrderStatusChange(pendingOrderStatus);
            return order; // early return, don't execute
        } else {
            if (order.getSide() == Side.BUY) {
                if (!hasSufficientFundsBuy(order)) {
                    Date today = new Date();
                    OrderStatusChange rejectedOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "insufficient funds", today);
                    order.addOrderStatusChange(rejectedOrderStatus);
                }
            } else if (order.getSide() == Side.SELL) {
                if (!hasSufficientHoldingsSell(order)) {
                    Date today = new Date();
                    OrderStatusChange rejectedOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "insufficient holdings", today);
                    order.addOrderStatusChange(rejectedOrderStatus);
                }
            }
            if (!instrumentIsTradeable(order)) {
                    Date today = new Date();
                    OrderStatusChange rejectedOrderStatus = new OrderStatusChange(OrderStatus.REJECTED, "instrument not tradable", today);
                    order.addOrderStatusChange(rejectedOrderStatus);
            }
        }
        return execute(order); // return true false instead for execute or not?
    }

    private Order execute(Order order) {
        // need idempotency here somehow

        // Client client = order.getClient();
        // update money
        portfolioService.updateCashHoldings(order); // we should have a specific method to create a holding to update cash based on the instrument order
        // update holdings
        portfolioService.updateHoldings(order); // just takes in order, portfolio service handles creating holding

        // update status - shoudl this happen based on portfolio service results above or is it just assumed thos will work?
        Date today = new Date();
        OrderStatusChange fulfilledOrderStatus = new OrderStatusChange(OrderStatus.FUFILLED, "order fulfilled", today);
        order.addOrderStatusChange(fulfilledOrderStatus);

        // // use orderreposiutory to update repository/db, since this order comes from contorller/frontend
        // this.repository.addOrder(order);

        // return back to frontend?

        return order;
    }


    // TODO: fix placeholders like date and stuff
    public OrderResponseDTO createOrderResponse(Order order) {
        // controller gets response back
        Integer orderId = order.getId();
        Integer clientId = order.getClient().userID;
        Integer portfolioId = order.getPortfolioID();
        String instrumentSymbol = order.getInstrument().getInstrumentSymbol();
        String instrumentFullName = order.getInstrument().getInstrumentFullName();
        InstrumentType instrumentType = order.getInstrument().getInstrumentType();
        String createdDate = order.getCreatedDate().toString();
        Double quantity = order.getQuantity();
        Double quotedPrice = order.getQuotedPrice();
        Side side = order.getSide();
        Double increaseThreshold = order.getIncreaseThreshold();
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
        this.repository.findByPortfolioId().map(OrderMapper::toDto).orElseThrow(() -> new ResourceNortFoundException("order not found"));
    }


    // method for processing after hours orders
    // not requested by front end
    public List<Order> processBatchOrders() {
        List<Order> afterHourOrders = new ArrayList<Order>(); // or whatever repository method returns
        afterHoursOrders = this.repository.findByStatus(OrderStatus.PENDING); //jpa knows its order table because its order repo
        List<Order> processedAfterHoursOrders = new ArrayList<Order>();
        for (Order order : afterHourOrders) {
            Order processedOrder = this.validate(order); // process (val + exec) instead, since validate shouldnt call eexecute anymore
            processedAfterHoursOrders.add(processedOrder);
        }
        return processedAfterHoursOrders;
    }



 
    
    // Buy Order
    private static boolean hasSufficientFundsBuy(Order order) throws Exception {
        double funds = portfolioService.getPortfolioById(order.getPortfolioID()).getUSDCash();
        double cost = order.getQuantity() * order.getQuotedPrice();
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
