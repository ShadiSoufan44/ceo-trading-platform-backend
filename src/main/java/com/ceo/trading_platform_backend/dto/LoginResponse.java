package com.ceo.trading_platform_backend.dto;

public record LoginResponse(
        Integer userId,
        String email,
        String token,
        String role
) {
}
