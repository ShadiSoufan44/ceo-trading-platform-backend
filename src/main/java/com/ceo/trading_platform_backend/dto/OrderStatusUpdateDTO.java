package com.ceo.trading_platform_backend.dto;

import java.util.UUID;

import com.ceo.trading_platform_backend.enums.OrderStatus;

// pushed to /topic/orders/{orderId} on every status change
public record OrderStatusUpdateDTO(
    UUID orderId,
    OrderStatus status,
    String message,
    String timestamp,
    boolean terminal // true once the order is FULFILLED, REJECTED or CANCELLED, no more updates follow
) {}
