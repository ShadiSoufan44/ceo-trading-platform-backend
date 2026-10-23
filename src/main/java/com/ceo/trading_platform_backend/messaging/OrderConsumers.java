package com.ceo.trading_platform_backend.messaging;

import java.util.Objects;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.services.OrderService;

import jakarta.transaction.Transactional;

@Service 
public class OrderConsumers {
    
    private KafkaTemplate<String, UUID> kafka;
    private OrderService orderService;

    // public OrderConsumers() {}

    public OrderConsumers(KafkaTemplate<String, UUID> kafkaTemplate, OrderService orderService) {
        this.kafka = kafkaTemplate;
        this.orderService = orderService;
    }

    @KafkaListener(topics = "orders.unvalidated", groupId = "validators")
    @Transactional 
    public void validate(UUID orderId) {
        System.out.println("UNVALIDATED: " + orderId);
        try {
            Order order = orderService.getOrderById(orderId).orElseThrow();
            if (order.getCurrentOrderStatus().getStatus() != OrderStatus.PENDING) {
                return;
            }
            order = orderService.validate(order);
            orderService.save(order);
            if (order.getCurrentOrderStatus().getStatus() == OrderStatus.ACCEPTED) {
                kafka.send(
                    "orders.unexecuted", Objects.requireNonNull(order.getOrderId())
                ).join();
            }
        } catch (RuntimeException exception) {
            orderService.markRejected(orderId, "order validation failed: " + exception.getMessage());
        }

    }

    @KafkaListener(topics = "orders.unexecuted", groupId = "executors")
    @Transactional 
    public void execute(UUID orderId) {
        System.out.println("Executing order " + orderId + "...");
        try {
            Order order = orderService.getOrderById(orderId).orElseThrow();
            if (order.getCurrentOrderStatus().getStatus() != OrderStatus.ACCEPTED) {
                return;
            }
            order = orderService.execute(order);
            orderService.save(order);
            System.out.println("Executed order " + orderId + "!");
        } catch (RuntimeException exception) {
            System.out.println("Execution Failed! " + exception.getMessage());
            orderService.markRejected(orderId, "order execution failed: " + exception.getMessage());
        }
    }
}
