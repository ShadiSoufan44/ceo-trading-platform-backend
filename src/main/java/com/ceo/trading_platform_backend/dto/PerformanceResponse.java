package com.ceo.trading_platform_backend.dto;
import java.math.BigDecimal;

public record PerformanceResponse(
    String date, // is this a stirng.. yes...
    BigDecimal balance
) {
}
