package com.ceo.trading_platform_backend.dto;

import com.ceo.trading_platform_backend.uml_objects.Enums.InstrumentType;
import com.ceo.trading_platform_backend.uml_objects.Enums.OrderStatus;
import com.ceo.trading_platform_backend.uml_objects.Enums.Side;

public record OrderResponseDTO(
    Integer orderId,
    Integer clientId,
    Integer portfolioId,
    String instrumentSymbol,
    InstrumentType instrumentType,
    String instrumentFullName,//should we remove this? 
    Double quantity,
    Double quotedPrice,
    Side side,
    Double increaseThreshold,
    String createdDate,
    String resolvedDate,
    OrderStatus currentStatus
) {}
