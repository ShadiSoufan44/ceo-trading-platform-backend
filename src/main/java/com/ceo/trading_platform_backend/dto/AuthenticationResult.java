package com.ceo.trading_platform_backend.dto;

public record AuthenticationResult(
        LoginResponse response,
        String refreshToken
) {
}