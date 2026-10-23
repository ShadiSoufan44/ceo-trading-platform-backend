package com.ceo.trading_platform_backend.dto;

import java.math.BigDecimal;

public record TopInstrumentInsight(
        String symbol,
        BigDecimal totalNotional,
        BigDecimal totalQuantity,
        long executedTrades) {
}
