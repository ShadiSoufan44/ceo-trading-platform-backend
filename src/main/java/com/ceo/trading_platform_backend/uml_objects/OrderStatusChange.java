package com.ceo.trading_platform_backend.uml_objects;

import java.util.Date;

import com.ceo.trading_platform_backend.uml_objects.Enums.OrderStatus;

public class OrderStatusChange {
    OrderStatus status;
    String message;
    Date date;
    public OrderStatusChange(OrderStatus status, String message, Date date) {
        this.status = status;
        this.message = message;
        this.date = date;
    }
}