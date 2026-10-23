package com.ceo.trading_platform_backend.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.ceo.trading_platform_backend.enums.InstrumentType;

// Contract shared by live and test market-data providers.
public interface MarketService {

    // public methods
    public BigDecimal getPriceWithType(InstrumentType type, String symbol);
    public QuoteData getQuoteByType(String symbol, InstrumentType type);
    public List<BatchQuoteResult> getQuotes(List<String> symbols);
    public SymbolMetadataData getSymbolMetadataByType(String symbol, InstrumentType type);
    public boolean isActiveSymbolByType(String symbol, InstrumentType type);
    public String normalizeSymbolByType(String symbol, InstrumentType type);
	public List<Candle> getCandlesByType(String symbol, InstrumentType type, LocalDate from, LocalDate to);
    // List<Candle> getCandles(String symbol, LocalDate from, LocalDate to);


    record QuoteData(
        String symbol,
        BigDecimal price,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal spreadBps,
        String currency,
        BigDecimal change,
        BigDecimal changePercent,
        BigDecimal previousClose,
        Instant asOf,
        String marketState) {
    }

    record BatchQuoteResult(
        String symbol, 
        String source, 
        Boolean stale, 
        QuoteData quote, 
        ApiError error) {
    }

    record SymbolMetadataData(
        String symbol,
        String name,
        String type,
        String exchange,
        String currency,
        Boolean active,
        Coverage coverage) {
    }

    record Coverage(LocalDate eodFrom, LocalDate eodTo) {
    }

    record Candle(
        LocalDate date,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal adjclose,
        Long volume,
        Boolean synthetic) {
    }

    record ApiError(String code, String message, java.util.Map<String, Object> details) {
    }
}
