package com.ceo.trading_platform_backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.enums.Side;

public record OrderRequestDTO(
    UUID clientId,
    String clientName,
    UUID portfolioId,
    String addInstrumentSymbol,
    InstrumentType addInstrumentType,
    String subtractInstrumentSymbol,
    InstrumentType subtractInstrumentType,
    BigDecimal quantity,
    BigDecimal quotedPrice,
    Side side,
    BigDecimal changeThreshold,
    String market
) {}
