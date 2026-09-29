package com.ceo.trading_platform_backend.dto;

import java.math.BigDecimal;
import java.util.Date;

public record HoldingResponse (
    int holdingId,
    Date dateCreated,
    int instrumentId,
    int orderId,
    BigDecimal purchasedPrice,
    BigDecimal quantity
) {
    
}
