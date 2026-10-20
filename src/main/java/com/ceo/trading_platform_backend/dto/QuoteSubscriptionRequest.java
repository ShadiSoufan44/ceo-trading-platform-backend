package com.ceo.trading_platform_backend.dto;

import com.ceo.trading_platform_backend.enums.InstrumentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuoteSubscriptionRequest(
    @NotBlank String symbol,
    @NotNull InstrumentType type
) {}
