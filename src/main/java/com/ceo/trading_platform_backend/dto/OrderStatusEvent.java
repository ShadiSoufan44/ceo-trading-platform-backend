package com.ceo.trading_platform_backend.dto;

import java.time.Instant;
import java.util.UUID;

import com.ceo.trading_platform_backend.enums.OrderStatus;

public record OrderStatusEvent(
    UUID orderId,
    UUID clientId,
    UUID portfolioId,
    OrderStatus status,
    String message,
    Instant statusDate,
    Instant createdDate
) {}
