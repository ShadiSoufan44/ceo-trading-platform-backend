package com.ceo.trading_platform_backend.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.fasterxml.jackson.databind.ObjectMapper;

// Fake market data for local development, so we can test without burning the market API's daily quota.
// Turn on with market.stub=true (e.g. in .env). Replaces MarketService everywhere it is injected.
// Prices are fixed per symbol and bid = ask = price, so orders never fail the change threshold check.
@Service
@Primary
@ConditionalOnProperty(name = "market.stub", havingValue = "true")
public class StubMarketService extends MarketService {

    private static final Map<String, BigDecimal> KNOWN_PRICES = Map.ofEntries(
        Map.entry("AAPL", new BigDecimal("227.50")),
        Map.entry("MSFT", new BigDecimal("415.20")),
        Map.entry("GOOGL", new BigDecimal("172.80")),
        Map.entry("TSLA", new BigDecimal("248.30")),
        Map.entry("RELIANCE", new BigDecimal("2950.00")),
        Map.entry("TCS", new BigDecimal("4100.00")),
        Map.entry("BTC", new BigDecimal("65000.00")),
        Map.entry("ETH", new BigDecimal("3200.00")),
        Map.entry("SOL", new BigDecimal("150.00")),
        Map.entry("USD", BigDecimal.ONE),
        Map.entry("INR", new BigDecimal("0.012")),
        Map.entry("EUR", new BigDecimal("1.08")),
        Map.entry("GBP", new BigDecimal("1.27"))
    );

    public StubMarketService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        super(restClientBuilder, objectMapper, "http://market-stub.invalid", "");
        System.out.println("WARNING: market.stub=true, using FAKE market prices (StubMarketService)");
    }

    @Override
    public BigDecimal getPriceWithType(InstrumentType type, String symbol) {
        return priceOf(symbol);
    }

    @Override
    public QuoteData getQuoteByType(String symbol, InstrumentType type) {
        return quoteOf(normalizeSymbolByType(symbol, type), priceOf(symbol));
    }

    @Override
    public List<BatchQuoteResult> getQuotes(List<String> symbols) {
        List<BatchQuoteResult> results = new ArrayList<>();
        for (String symbol : symbols) {
            results.add(new BatchQuoteResult(symbol, "stub", false, quoteOf(symbol, priceOf(symbol)), null));
        }
        return results;
    }

    @Override
    public SymbolMetadataData getSymbolMetadataByType(String symbol, InstrumentType type) {
        String normalized = normalizeSymbolByType(symbol, type);
        return new SymbolMetadataData(normalized, baseSymbol(symbol), type.name(), "STUB", "USD", true, null);
    }

    @Override
    public boolean isActiveSymbolByType(String symbol, InstrumentType type) {
        return true;
    }

    // one candle per day, drifting a little around the fixed price so charts have some shape
    @Override
    public List<Candle> getCandles(String symbol, LocalDate from, LocalDate to) {
        LocalDate end = (to != null) ? to : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(30);
        BigDecimal price = priceOf(symbol);
        List<Candle> candles = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            BigDecimal drift = BigDecimal.ONE.add(BigDecimal.valueOf(Math.sin(date.toEpochDay()) * 0.02));
            BigDecimal close = price.multiply(drift).setScale(4, RoundingMode.HALF_UP);
            candles.add(new Candle(date, close, close, close, close, close, 1_000_000L, true));
        }
        return candles;
    }

    private QuoteData quoteOf(String symbol, BigDecimal price) {
        return new QuoteData(symbol, price, price, price, BigDecimal.ZERO, "USD",
            BigDecimal.ZERO, BigDecimal.ZERO, price, Instant.now(), "OPEN");
    }

    private BigDecimal priceOf(String symbol) {
        String base = baseSymbol(symbol);
        BigDecimal known = KNOWN_PRICES.get(base);
        if (known != null) {
            return known;
        }
        // unknown symbols get a stable made-up price between 10 and 500
        return BigDecimal.valueOf(10 + Math.floorMod(base.hashCode(), 491));
    }

    // "X:BTC-USD" -> "BTC", "FX:EURUSD" -> "EUR", " aapl " -> "AAPL"
    private String baseSymbol(String symbol) {
        String normalized = symbol.trim().toUpperCase();
        if (normalized.startsWith("X:")) {
            normalized = normalized.substring(2).replace("-USD", "");
        } else if (normalized.startsWith("FX:")) {
            normalized = normalized.substring(3);
            if (normalized.length() > 3 && normalized.endsWith("USD")) {
                normalized = normalized.substring(0, normalized.length() - 3);
            }
        }
        return normalized;
    }
}
