package com.ceo.trading_platform_backend.messaging;

import java.util.Objects;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

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
        Order order = orderService.getOrderById(orderId).orElseThrow();
        orderService.validate(order);
        kafka.send("orders.unexecuted", Objects.requireNonNull(order.getOrderId())).join();
    }

    @KafkaListener(topics = "orders.unexecuted", groupId = "executors")
    @Transactional 
    public void execute(UUID orderId) {
        System.out.println("UNEXECUTED: " + orderId);
        Order order = orderService.getOrderById(orderId).orElseThrow();
        System.out.println("UNEXECUTED2: " + orderId);
        orderService.execute(order);
    }
}




//b1d23821-511c-47ab-a283-5ec10ef02614
//b1d23821-511c-47ab-a283-5ec10ef02614