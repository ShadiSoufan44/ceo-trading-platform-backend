package com.ceo.trading_platform_backend.dto;

import java.util.List;
import java.util.UUID;

import com.ceo.trading_platform_backend.enums.PortfolioType;

public record PortfolioResponse (
    UUID ID,
    String name,
    List<UUID> holdingIds,
    PortfolioType type
) {  
}
