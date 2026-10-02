package com.ceo.trading_platform_backend.models;

import java.util.UUID;
import java.time.Instant;

import com.ceo.trading_platform_backend.enums.OrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity 
public class OrderStatusChange {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_status_change_id")
    private UUID orderStatusChangeId;

    @Column(name = "status")
    private OrderStatus status;

    @Column(name = "message")
    private String message;

    @Column(name = "change_date")
    Instant date;

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