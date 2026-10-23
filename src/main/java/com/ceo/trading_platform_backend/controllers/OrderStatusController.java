package com.ceo.trading_platform_backend.controllers;

import java.util.UUID;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import com.ceo.trading_platform_backend.dto.OrderStatusUpdateDTO;
import com.ceo.trading_platform_backend.services.OrderService;

@Controller
public class OrderStatusController {

    private final OrderService orderService;

    public OrderStatusController(OrderService orderService) {
        this.orderService = orderService;
    }

    // a client that subscribes after the order already moved on (e.g. validated before the POST response arrived)
    // gets the current status straight back, so it never sits on a stale PENDING
    @SubscribeMapping("/orders/{orderId}")
    public OrderStatusUpdateDTO currentStatus(@DestinationVariable UUID orderId) {
        return orderService.getCurrentStatusUpdate(orderId);
    }
}
