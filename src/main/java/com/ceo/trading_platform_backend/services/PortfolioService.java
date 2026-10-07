package com.ceo.trading_platform_backend.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ceo.trading_platform_backend.enums.Side;
import com.ceo.trading_platform_backend.exception.ResourceNotFoundException;
import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.Portfolio;
import com.ceo.trading_platform_backend.repositories.PortfolioRepository;

import jakarta.transaction.Transactional;

@Service 
public class PortfolioService {
    private final PortfolioRepository repository;
    private final InstrumentService instrumentService;
    private final MarketService marketService;

    private final String CURRENCY_SYMBOL = "USD";

    public PortfolioService(
        PortfolioRepository repository,
        InstrumentService instrumentService,
        MarketService marketService
    ) {
        this.repository = repository;
        this.instrumentService = instrumentService;
        this.marketService = marketService;
    }

    private BigDecimal getPortfolioBuyingPower(Portfolio portfolio) {
        BigDecimal totalValue = new BigDecimal(0);
        for (Holding holding : portfolio.getHoldings()) {
            if (holding.getInstrument().getSymbol().equals(CURRENCY_SYMBOL)) {
                totalValue = totalValue.add(holding.getQuantity());
            }
        }
        return totalValue;
    }

    private BigDecimal getPortfolioTotalValue(Portfolio portfolio) {
        BigDecimal value = new BigDecimal(0);

        for (Holding holding : portfolio.getHoldings()) {
            BigDecimal price;
            if (holding.getInstrument().getSymbol().equals(CURRENCY_SYMBOL)) {
                price = new BigDecimal(1);
            } else {
                price = marketService.getPrice(holding.getInstrument().getSymbol());
            }
            value = value.add(price.multiply(holding.getQuantity()));
        }
        return value;
    }

    public Portfolio getPortfolioById(UUID portfolioId) {
        try {
            Portfolio portfolio = repository.getReferenceById(portfolioId);
            return portfolio;
        } catch (Exception e) {
            throw new ResourceNotFoundException("Portfolio not found with id " + portfolioId);
        }
    }

  
    
    public BigDecimal getBuyingPowerByPortfolioId(UUID portfolioId) {
        Portfolio portfolio = getPortfolioById(portfolioId);
        return getPortfolioBuyingPower(portfolio);
    }

    public BigDecimal getTotalValueByPortfolioId(UUID portfolioId) {
        Portfolio portfolio = getPortfolioById(portfolioId);
        return getPortfolioTotalValue(portfolio);
    }

    public List<Holding> getHoldingsByPortfolioId(UUID portfolioId) {
        Portfolio portfolio = getPortfolioById(portfolioId);
        if (portfolio == null) return null;
        return portfolio.getHoldings();
    }
    
    @Transactional 
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
            BigDecimal.ONE,
            totalCashQuantity,
            cashInstrument,
            order,
            portfolio
        );

        Holding tradeHolding = new Holding(
            now,
            instrumentPrice,
            instrumentQuantity,
            tradeInstrument,
            order,
            portfolio
        );

        portfolio = repository.save(portfolio);
        portfolio.addHolding(cashHolding);
        portfolio.addHolding(tradeHolding);
        repository.save(portfolio);
    }
}
