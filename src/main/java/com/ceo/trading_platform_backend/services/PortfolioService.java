package com.ceo.trading_platform_backend.services;

import com.ceo.trading_platform_backend.repositories.PortfolioRepository;

public class PortfolioService {
    private final PortfolioRepository repository;

    public PortfolioService(PortfolioRepository repository) {
        this.repository = repository;
    }

    
    
}
