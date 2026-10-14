package com.ceo.trading_platform_backend.services;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ceo.trading_platform_backend.models.Portfolio;
import com.ceo.trading_platform_backend.repositories.PortfolioRepository;

/**
 * ClientService handles client-specific business logic.
 * All methods operate on data scoped to a single client.
 */
@Service
public class ClientService {
    
    private final PortfolioRepository portfolioRepository;

    public ClientService(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    /**
     * Get all portfolios for a client.
     */
    @Transactional(readOnly = true)
    public List<Portfolio> getPortfolios(UUID clientId) {
        List<Portfolio> portfolios = portfolioRepository.findByClientUserId(clientId);
        return portfolios == null ? List.of() : portfolios;
    }

}
