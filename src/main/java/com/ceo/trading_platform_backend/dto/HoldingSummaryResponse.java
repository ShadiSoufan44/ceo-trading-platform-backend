package com.ceo.trading_platform_backend.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record HoldingSummaryResponse(
    UUID holdingId,
    UUID portfolioId,
    String symbol,
    String companyName,
    BigDecimal shares,
    BigDecimal avgCost,
    BigDecimal currentPrice,
    BigDecimal dayChangeValue,
    Map<String, BigDecimal> percentChange,
    BigDecimal weekLow52,
    BigDecimal weekHigh52
) {
}
