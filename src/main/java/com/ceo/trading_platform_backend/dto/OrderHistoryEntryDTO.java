package com.ceo.trading_platform_backend.dto;

import java.time.Instant;
import java.util.UUID;

import com.ceo.trading_platform_backend.enums.OrderStatus;

/** One recorded status change; repeated statuses are separate entries, not deduplicated. */
public record OrderHistoryEntryDTO(
        UUID orderStatusChangeId,
        UUID orderId,
        UUID clientId,
        OrderStatus status,
        String message,
        Instant timestamp
) {}