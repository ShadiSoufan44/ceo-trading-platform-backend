package com.ceo.trading_platform_backend.dto;

import java.util.UUID;

public record LoginResponse(
        UUID userId,
        String email,
        String token,
        String role
) {
}
