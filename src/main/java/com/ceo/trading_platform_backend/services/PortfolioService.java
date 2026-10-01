package com.ceo.trading_platform_backend.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.Portfolio;
import com.ceo.trading_platform_backend.repositories.PortfolioRepository;
import com.ceo.trading_platform_backend.uml_objects.Enums.Side;

@Service 
public class PortfolioService {
    private final PortfolioRepository repository;
    private final InstrumentService instrumentService;
    // FIXME private final MarketService marketService;

    public PortfolioService(
        PortfolioRepository repository
        , InstrumentService instrumentService
        // FIXME , MarketService marketService
    ) {
        this.repository = repository;
        this.instrumentService = instrumentService;
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
    // public List<Portfolio> getPortfoliosByClientId(int clientId) {
    //     List<Portfolio> portfolios = repository.findByClientId(clientId);
    //     if (portfolios == null) return null;
    //     // if (portfolios.size() == 0) return null;
    //     return portfolios;
    // }
    
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

    public List<Holding> getHoldingsByPortfolioId(int portfolioId) {
        Portfolio portfolio = getPortfolioById(portfolioId);
        if (portfolio == null) return null;
        return portfolio.getHoldings();
    }
    
    public void updateHoldingsFromOrder(Order order, BigDecimal instrumentPrice) {
        Portfolio portfolio = getPortfolioById(order.getPortfolioId());
        Side side = order.getSide();

        BigDecimal totalCashQuantity;
        BigDecimal instrumentQuantity;
        if (side == Side.BUY) {
            totalCashQuantity = instrumentPrice.negate().multiply(order.getQuantity());
            instrumentQuantity = order.getQuantity();
        } else {
            totalCashQuantity = instrumentPrice.multiply(order.getQuantity());
            instrumentQuantity = order.getQuantity().negate();
        }

        Instrument tradeInstrument = instrumentService.getInstrumentById(order.getInstrumentId());
        Instrument cashInstrument = instrumentService.getInstrumentBySymbol("USD");

        Instant now = Instant.now();

        Holding cashHolding = new Holding(
            now,
            new BigDecimal(0),
            totalCashQuantity,
            cashInstrument,
            order
        );

        Holding tradeHolding = new Holding(
            now,
            instrumentPrice,
            instrumentQuantity,
            tradeInstrument,
            order
        );

        portfolio.addHolding(cashHolding);
        portfolio.addHolding(tradeHolding);
        repository.save(portfolio);
    }
}
