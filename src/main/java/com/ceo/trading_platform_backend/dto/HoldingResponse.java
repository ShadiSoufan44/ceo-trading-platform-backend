package com.ceo.trading_platform_backend.dto;

import java.util.Date;

public record HoldingResponse (
    int holdingId,
    Date dateCreated,
    int instrumentId,
    int orderId,
    double purchasedPrice,
    double quantity
) {
    
}
