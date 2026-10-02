package com.ceo.trading_platform_backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.enums.Side;

public record OrderResponseDTO(
    UUID orderId,
    UUID clientId,
    UUID portfolioId,
    String instrumentSymbol,
    InstrumentType instrumentType,
    String instrumentFullName,//should we remove this? 
    BigDecimal quantity,
    BigDecimal quotedPrice,
    Side side,
    BigDecimal increaseThreshold,
    String createdDate,
    String resolvedDate,
    OrderStatus currentStatus
) {}
