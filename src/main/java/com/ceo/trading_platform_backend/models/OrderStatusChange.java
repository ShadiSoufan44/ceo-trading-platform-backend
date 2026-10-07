package com.ceo.trading_platform_backend.models;

import java.util.UUID;
import java.time.Instant;

import com.ceo.trading_platform_backend.enums.OrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity 
public class OrderStatusChange {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_status_change_id")
    private UUID orderStatusChangeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;

    @Column(name = "message")
    private String message;

    @Column(name = "change_date")
    private Instant date;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    public OrderStatusChange() {}

    public OrderStatusChange(OrderStatus status, String message, Instant date) {
        this.status = status;
        this.message = message;
        this.date = date;
    }

    public OrderStatus getStatus() {
        return this.status;
    }

    public Instant getDate() {
        return this.date;
    }
}