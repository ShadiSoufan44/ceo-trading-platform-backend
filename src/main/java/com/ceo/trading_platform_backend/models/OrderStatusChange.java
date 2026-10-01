package com.ceo.trading_platform_backend.models;

import java.time.LocalDateTime;

import com.ceo.trading_platform_backend.enums.OrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity 
public class OrderStatusChange {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_status_change_id")
    Integer orderStatusChangeId;

    @Column(name = "status")
    OrderStatus status;

    @Column(name = "message")
    String message;

    @Column(name = "change_date")
    LocalDateTime date;

    public OrderStatusChange(OrderStatus status, String message, LocalDateTime date) {
        this.status = status;
        this.message = message;
        this.date = date;
    }

    public OrderStatus getStatus() {
        return this.status;
    }

    public LocalDateTime getDate() {
        return this.date;
    }
}