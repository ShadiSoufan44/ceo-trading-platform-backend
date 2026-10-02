package com.ceo.trading_platform_backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID userId,
        String fullName,
        String email,
        OffsetDateTime joinDate,
        String role
) {
}
