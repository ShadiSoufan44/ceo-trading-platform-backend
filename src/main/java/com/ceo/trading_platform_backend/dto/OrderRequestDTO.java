package com.ceo.trading_platform_backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.ceo.trading_platform_backend.enums.Side;

public record OrderRequestDTO(
    UUID clientId,
    String clientName,
    UUID portfolioId,
    String instrumentSymbol,
    BigDecimal quantity,
    BigDecimal quotedPrice,
    Side side,
    BigDecimal increaseThreshold
) {}
