package com.ceo.trading_platform_backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HoldingResponse (
    UUID holdingId,
    Instant dateCreated,
    UUID instrumentId,
    UUID orderId,
    BigDecimal purchasedPrice,
    BigDecimal quantity
) {
    
}
