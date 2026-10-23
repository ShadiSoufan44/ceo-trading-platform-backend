package com.ceo.trading_platform_backend.messaging;

import java.util.Objects;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
                sendAfterCommit("orders.unexecuted", Objects.requireNonNull(order.getOrderId()));
            }
        } catch (RuntimeException exception) {
            orderService.markRejected(orderId, "order validation failed: " + exception.getMessage());
        }

    }

    // the executor re-reads the order and skips it unless it is ACCEPTED, so it must not get the id
    // before ACCEPTED is committed, otherwise it sees PENDING and the order is stuck at ACCEPTED
    private void sendAfterCommit(String topic, UUID orderId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            kafka.send(topic, orderId).join();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafka.send(topic, orderId).join();
            }
        });
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
