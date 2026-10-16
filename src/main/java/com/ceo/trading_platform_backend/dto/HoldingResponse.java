package com.ceo.trading_platform_backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ceo.trading_platform_backend.enums.InstrumentType;

public record HoldingResponse (
    UUID holdingId,
    Instant dateCreated,
    UUID instrumentId,
    String instrumentSymbol,
    String instrumentName,
    InstrumentType instrumentType,
    UUID orderId,
    BigDecimal purchasedPrice,
    BigDecimal quantity
) {
    
}
