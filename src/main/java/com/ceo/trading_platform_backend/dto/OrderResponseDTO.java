package com.ceo.trading_platform_backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.enums.Side;

public record OrderResponseDTO (
    UUID orderId,
    UUID clientId,
    UUID portfolioId,
    String addInstrumentSymbol,
    InstrumentType addInstrumentType,
    String addInstrumentFullName,//should we remove this? 
    String subtractInstrumentSymbol,
    InstrumentType subtractInstrumentType,
    String subtractInstrumentFullName,
    BigDecimal quantity,
    BigDecimal quotedPrice,
    BigDecimal resolvedPrice,
    Side side,
    BigDecimal changeThreshold,
    String createdDate,
    String resolvedDate,
    OrderStatus currentStatus
) {}
