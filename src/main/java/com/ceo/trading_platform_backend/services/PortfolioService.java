package com.ceo.trading_platform_backend.services;

import java.util.ArrayList;
import java.util.List;

import com.ceo.trading_platform_backend.dto.HoldingResponse;
import com.ceo.trading_platform_backend.dto.PortfolioResponse;
import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Portfolio;
import com.ceo.trading_platform_backend.repositories.PortfolioRepository;

public class PortfolioService {
    private final PortfolioRepository repository;

    public PortfolioService(PortfolioRepository repository) {
        this.repository = repository;
    }
    
    private PortfolioResponse createPortfolioResponse(Portfolio portfolio) {
        if (portfolio == null) return null;

        List<Integer> holdingIds = new ArrayList<>();

        for (Holding holding : portfolio.getHoldings()) {
            holdingIds.add(holding.getID());
        }
        return new PortfolioResponse(
            portfolio.getPortfolioId(), holdingIds, portfolio.getType()
        );
    }
    
    private HoldingResponse createHoldingResponse(Holding holding) {
        if (holding == null) return null;
        return new HoldingResponse(
            holding.getID(),
            holding.getDateCreated(), 
            holding.getInstrument().getID(),
            holding.getOrder().getOrderId(),
            holding.getPurchasedPrice(),
            holding.getQuantity()
        );
    }

    private Portfolio getPortfolioById(int portfolioId) {
        return repository.findById(portfolioId).orElse(null);
    }

    // TODO: Move this to ClientService.java
    public List<PortfolioResponse> getPortfoliosByClientId(int clientId) {
        List<Portfolio> portfolios = repository.findByClientId(clientId);
        List<PortfolioResponse> result = new ArrayList<>();
        for (Portfolio portfolio : portfolios) {
            result.add(createPortfolioResponse(portfolio));
        }
        if (result.size() == 0) return null;
        return result;
    }
    

    public PortfolioResponse getPortfolioResponseById(int portfolioId) {
        Portfolio portfolio = repository.findById(portfolioId).orElse(null);
        if (portfolio == null) return null;
        return createPortfolioResponse(portfolio);
    }

    public Double getBuyingPowerByPortfolioId(int portfolioId) {
        Portfolio portfolio = getPortfolioById(portfolioId);
        if (portfolio == null) return null;
        return portfolio.getPortfolioBuyingPower();
    }

    public List<HoldingResponse> getHoldingsByPortfolioId(int portfolioId) {
        List<HoldingResponse> result = new ArrayList<>();
        for (Holding holding : getPortfolioById(portfolioId).getHoldings()) {
            result.add(createHoldingResponse(holding));
        }
        if (result.size() == 0) return null;
        return result;
    }
    
}
