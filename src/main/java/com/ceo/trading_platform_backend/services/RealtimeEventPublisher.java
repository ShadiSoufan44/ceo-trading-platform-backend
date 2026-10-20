package com.ceo.trading_platform_backend.services;

import java.util.Objects;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.ceo.trading_platform_backend.dto.OrderStatusEvent;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.OrderStatusChange;

@Service
public class RealtimeEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishOrderUpdate(Order order) {
        if (order == null || order.getOrderId() == null || order.getClientId() == null) {
            return;
        }

        OrderStatusChange current = Objects.requireNonNull(order.getCurrentOrderStatus());
        OrderStatusEvent event = new OrderStatusEvent(
            order.getOrderId(),
            order.getClientId(),
            order.getPortfolioId(),
            current.getStatus(),
            current.getMessage(),
            current.getDate(),
            order.getCreatedDate()
        );

        messagingTemplate.convertAndSend("/topic/users/" + order.getClientId() + "/orders", event);
        messagingTemplate.convertAndSend("/topic/orders/" + order.getOrderId(), event);
    }
}
