package com.ceo.trading_platform_backend.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ceo.trading_platform_backend.dto.HoldingResponse;
import com.ceo.trading_platform_backend.dto.PortfolioResponse;
import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Portfolio;
import com.ceo.trading_platform_backend.repositories.PortfolioRepository;

@Service 
public class PortfolioService {
    private final PortfolioRepository repository;
    // FIXME private final MarketService marketService;

    public PortfolioService(
        PortfolioRepository repository
        // FIXME , MarketService marketService
    ) {
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

    private BigDecimal getPortfolioBuyingPower(Portfolio portfolio) {
        BigDecimal totalValue = new BigDecimal(0);
        for (Holding holding : portfolio.getHoldings()) {
            if (holding.getInstrument().getSymbol() == "USD") {
                totalValue = totalValue.add(holding.getQuantity());
            }
        }
        return totalValue;
    }

    private BigDecimal getPortfolioTotalValue(Portfolio portfolio) {
        BigDecimal value = getPortfolioBuyingPower(portfolio);

        for (Holding holding : portfolio.getHoldings()) {
            // FIXME: BigDecimal price = marketService.getPrice(holding.getInstrument());
            BigDecimal price = new BigDecimal(1);  
            value = value.add(price.multiply(holding.getQuantity()));
        }
        return value;
    }

    public Portfolio getPortfolioById(int portfolioId) {
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

    public BigDecimal getBuyingPowerByPortfolioId(int portfolioId) {
        Portfolio portfolio = getPortfolioById(portfolioId);
        if (portfolio == null) return null;
        return getPortfolioBuyingPower(portfolio);
    }

    public BigDecimal getTotalValueByPortfolioId(int portfolioId) {
        Portfolio portfolio = getPortfolioById(portfolioId);
        if (portfolio == null) return null;
        return getPortfolioTotalValue(portfolio);
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
