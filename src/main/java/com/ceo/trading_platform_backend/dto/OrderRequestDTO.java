package com.ceo.trading_platform_backend.dto;

import java.math.BigDecimal;

import com.ceo.trading_platform_backend.uml_objects.Enums.Side;

public record OrderRequestDTO(
    Integer clientId,
    String clientName,
    Integer portfolioId,
    String instrumentSymbol,
    BigDecimal quantity,
    BigDecimal quotedPrice,
    Side side,
    BigDecimal increaseThreshold
) {}
