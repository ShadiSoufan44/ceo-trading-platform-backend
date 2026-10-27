package com.ceo.trading_platform_backend.dto;

import java.util.UUID;

import com.ceo.trading_platform_backend.enums.PortfolioType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePortfolioRequest(
        @NotBlank(message = "Portfolio name is required")
        @Size(max = 100, message = "Portfolio name must be at most 100 characters")
        String name,
        @NotNull(message = "Portfolio type is required")
        PortfolioType type,
        @NotNull(message = "Client ID is required")
        UUID clientId
) {
}