package com.ceo.trading_platform_backend.dto;

import java.math.BigDecimal;

public record TopMover(
        String symbol,
        BigDecimal currentPrice,
        BigDecimal previousClose,
        BigDecimal percentageChange) {
}