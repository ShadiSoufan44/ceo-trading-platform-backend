package com.ceo.trading_platform_backend.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID userId,
        String fullName,
        String email,
        Instant joinDate,
        String role
) {
}
