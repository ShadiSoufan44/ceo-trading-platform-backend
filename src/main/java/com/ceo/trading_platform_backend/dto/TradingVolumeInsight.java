package com.ceo.trading_platform_backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TradingVolumeInsight(
        Instant from,
        Instant to,
        String symbol,
        String clientSegment,
        BigDecimal totalNotional,
        BigDecimal totalQuantity,
        long executedTrades) {
}
