package com.ceo.trading_platform_backend.messaging;

import java.time.Instant;
import java.util.UUID;

import com.ceo.trading_platform_backend.enums.OrderStatus;

// raised by Order.addOrderStatusChange, published when the order is saved
public record OrderStatusChangedEvent(
    UUID orderId,
    OrderStatus status,
    String message,
    Instant date
) {}
