package com.ceo.trading_platform_backend.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ceo.trading_platform_backend.enums.InstrumentType;
@Service // UNCOMMMENT THIS FOR MOCK MARKET SERVICE AND COMMENT IN REAL
public class MockMarketService implements com.ceo.trading_platform_backend.services.MarketService{
    
    public MockMarketService() {} // idk what does in constructor for mock

    // defining methods
    public BigDecimal getPriceWithType(InstrumentType type, String symbol) {
        return new BigDecimal("100"); // everything is going to be $100
    }

    public QuoteData getQuoteByType(String symbol, InstrumentType type) {
        QuoteData quote = new QuoteData(
            symbol, 
            new BigDecimal("95"), 
            new BigDecimal("95"), 
            new BigDecimal("95"), 
            new BigDecimal("95"), 
            "USD", 
            new BigDecimal("0"), 
            new BigDecimal("0"),
            new BigDecimal("95"),
            java.time.Instant.now(), 
            "OPEN"
        ); // not sure if this is right
        return quote;
    }

    public List<BatchQuoteResult> getQuotes(List<String> symbols) {
    
        if (symbols == null || symbols.isEmpty()) {
			throw new IllegalArgumentException("At least one symbol is required.");
		}
		if (symbols.size() > 25) { // idk
			throw new IllegalArgumentException("The quotes endpoint accepts at most 25 symbols per request.");
		}
        List<BatchQuoteResult> results = new ArrayList<>();
        for (String symbol : symbols) {
            QuoteData quote = new QuoteData(
                symbol, 
                new BigDecimal("95"), 
                new BigDecimal("95"), 
                new BigDecimal("95"), 
                new BigDecimal("95"), 
                "USD", 
                new BigDecimal("0"), 
                new BigDecimal("0"),
                new BigDecimal("95"),
                java.time.Instant.now(), 
                "OPEN"
            );
            BatchQuoteResult result = new BatchQuoteResult(
                symbol,
                "source??",
                false, //not stale
                quote,
                null
            );
            results.add(result);
        }
        return results;
    }

    public SymbolMetadataData getSymbolMetadataByType(String symbol, InstrumentType type) {
        SymbolMetadataData metadata = new SymbolMetadataData(
            symbol,
            "Mock Name",
            type.toString(),
            "Mock Exchange",
            "USD",
            true,
            new Coverage(LocalDate.now().minusYears(1), LocalDate.now())
        );
        return metadata;
    }

    public List<Candle> getCandlesByType(String symbol, InstrumentType instrumentType, LocalDate from, LocalDate to) {
        return getCandles(normalizeSymbolByType(symbol, instrumentType), from, to);
    }

    private List<Candle> getCandles(String symbol, LocalDate from, LocalDate to) {
        List<Candle> candles = new ArrayList<>();
        LocalDate current = from;
        while (!current.isAfter(to)) {
            Candle candle = new Candle(
                current,
                new BigDecimal("95"),
                new BigDecimal("95"),
                new BigDecimal("95"),
                new BigDecimal("95"),
                new BigDecimal("95"),
                1000L,
                false
            );
            candles.add(candle);
            current = current.plusDays(1);
        }
        return candles;
    }
    
    public boolean isActiveSymbolByType(String symbol, InstrumentType type) {
        return true; // matches active in symbol metadat
    }
    
    public String normalizeSymbolByType(String symbol, InstrumentType type) {
        return symbol.toUpperCase(); // idk what normalization has to be done if its not being sent to api
    }

}
