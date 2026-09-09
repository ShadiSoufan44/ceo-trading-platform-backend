package com.ceo.trading_platform_backend.user.dto;

import java.time.OffsetDateTime;

public record UserResponse(
        Integer userId,
        String fullName,
        String email,
        OffsetDateTime joinDate,
        String role
) {
}
