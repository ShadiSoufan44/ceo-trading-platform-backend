package com.ceo.trading_platform_backend.dto;

import java.time.Instant;

public record ChangePasswordResponse(
        String message,
        Instant changedAt
) {
}