package com.ceo.trading_platform_backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.ceo.trading_platform_backend.enums.InstrumentType;

public record QuoteUpdateEvent(
    String requestedSymbol,
    InstrumentType instrumentType,
    String marketSymbol,
    BigDecimal price,
    BigDecimal bid,
    BigDecimal ask,
    BigDecimal change,
    BigDecimal changePercent,
    BigDecimal previousClose,
    String currency,
    Instant asOf,
    String marketState
) {}
