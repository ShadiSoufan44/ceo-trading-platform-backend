package com.ceo.trading_platform_backend.dto;

import java.math.BigDecimal;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.enums.Side;

public record OrderResponseDTO(
    Integer orderId,
    Integer clientId,
    Integer portfolioId,
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
