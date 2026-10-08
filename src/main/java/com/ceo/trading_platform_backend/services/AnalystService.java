package com.ceo.trading_platform_backend.services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ceo.trading_platform_backend.repositories.OrderRepository;

/**
 * AnalystService handles analyst-specific reporting and market data operations.
 */
@Service
public class AnalystService {
    
    private final MarketService marketService;

    public AnalystService(MarketService marketService) {
        this.marketService = marketService;
    }

    /**
     * Get historical price data for a symbol.
     */
    @Transactional(readOnly = true)
    public List<MarketService.Candle> getHistoricalData(String symbol) {
        return marketService.getCandles(symbol, null, null);
    }
}
