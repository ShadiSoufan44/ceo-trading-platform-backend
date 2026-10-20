package com.ceo.trading_platform_backend.messaging;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.OrderStatusChange;
import com.ceo.trading_platform_backend.repositories.OrderRepository;
import com.ceo.trading_platform_backend.services.RealtimeEventPublisher;

@Service
public class OrderProducers {
    
    private KafkaTemplate<String, UUID> kafka;
    private OrderRepository repository;
    private RealtimeEventPublisher realtimeEventPublisher;

    // public OrderProducers() {}

    public OrderProducers(
        OrderRepository repository,
        KafkaTemplate<String, UUID> kafkaTemplate,
        RealtimeEventPublisher realtimeEventPublisher
    ) {
        this.kafka = kafkaTemplate;
        this.repository = repository;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    public void submit(Order order) {
        UUID id = Objects.requireNonNull(order.getOrderId());
        Instant today = Instant.now();
        OrderStatusChange pendingOrderStatus = new OrderStatusChange(OrderStatus.PENDING, "order submitted to be processed", today);
        order.addOrderStatusChange(pendingOrderStatus);
        repository.save(order);
        realtimeEventPublisher.publishOrderUpdate(order);
        kafka.send("orders.unvalidated", id).join();
    }

}
