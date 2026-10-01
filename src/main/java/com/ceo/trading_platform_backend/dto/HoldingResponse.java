package com.ceo.trading_platform_backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record HoldingResponse (
    int holdingId,
    Instant dateCreated,
    int instrumentId,
    int orderId,
    BigDecimal purchasedPrice,
    BigDecimal quantity
) {
    
}
