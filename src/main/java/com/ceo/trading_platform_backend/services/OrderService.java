package com.ceo.trading_platform_backend.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ceo.trading_platform_backend.uml_objects.Client;
import com.ceo.trading_platform_backend.uml_objects.Holding;
import com.ceo.trading_platform_backend.uml_objects.Instrument;
import com.ceo.trading_platform_backend.uml_objects.Order;
import com.ceo.trading_platform_backend.uml_objects.OrderStatusChange;
import com.ceo.trading_platform_backend.uml_objects.Enums.OrderStatus;
import com.ceo.trading_platform_backend.uml_objects.Enums.Side;

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

    public static Order createOrder(OrderRequestDTO orderRequest) {
        Client client = clientService.getUserById(orderRequest.clientId());
        int portfolioId = portfolioService.getPortfolioById(orderRequest.portfolioId());
        Instrument instrument = instrumentService.getInstrumentById(orderRequest.instrumentSymbol()); // instrument servcie asks external api for instrument info
        Date createdDate = new Date();
        double quantity = orderRequest.quantity();
        double quotedPrice = orderRequest.quotedPrice();
        Side side = orderRequest.side();
        double increaseThreshold = orderRequest.increaseThreshold();
        Order order = new Order(client, instrument, portfolioId, createdDate, quantity, quotedPrice, increaseThreshold);
    }

    public static Order validate(Order order) {
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

        return execute(order);
    }

    public static Order submit(OrderRequestDTO orderRequest) {
        Order order = createOrder(orderRequest);
        return validate(order);
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



    private static Order execute(Order order) {
        // need idempotency here somehow

        Client client = order.getClient();

        

        return order;
    }
}
