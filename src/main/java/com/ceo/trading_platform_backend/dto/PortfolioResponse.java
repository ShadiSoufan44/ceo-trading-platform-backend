package com.ceo.trading_platform_backend.dto;

import java.util.List;

import com.ceo.trading_platform_backend.enums.PortfolioType;

public record PortfolioResponse (
    int ID,
    List<Integer> holdingIds,
    PortfolioType type
) {  
}
