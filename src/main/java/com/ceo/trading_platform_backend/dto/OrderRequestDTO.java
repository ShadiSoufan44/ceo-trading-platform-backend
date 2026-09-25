package com.ceo.trading_platform_backend.dto;

import com.ceo.trading_platform_backend.uml_objects.Enums.Side;

public record OrderRequestDTO(
    Integer clientId,
    String clientName,
    Integer portfolioId,
    String instrumentSymbol,
    Double quantity,
    Double quotedPrice,
    Side side,
    Double increaseThreshold
) {}
