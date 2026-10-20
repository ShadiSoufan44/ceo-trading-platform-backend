package com.ceo.trading_platform_backend.services;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ceo.trading_platform_backend.dto.QuoteUpdateEvent;
import com.ceo.trading_platform_backend.enums.InstrumentType;

@Service
public class QuoteStreamingService {

    private static final Duration SUBSCRIPTION_TTL = Duration.ofMinutes(15);

    private final MarketService marketService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, Instant> trackedSymbols = new ConcurrentHashMap<>();
    private final long publishIntervalMs;

    public QuoteStreamingService(
        MarketService marketService,
        SimpMessagingTemplate messagingTemplate,
        @Value("${market.stream.interval-ms:5000}") long publishIntervalMs
    ) {
        this.marketService = marketService;
        this.messagingTemplate = messagingTemplate;
        this.publishIntervalMs = publishIntervalMs;
    }

    public void trackSymbol(String symbol, InstrumentType type) {
        if (symbol == null || symbol.isBlank() || type == null) {
            return;
        }
        String normalizedSymbol = normalize(symbol);
        trackedSymbols.put(key(normalizedSymbol, type), Instant.now());
        publishSymbol(normalizedSymbol, type);
    }

    @Scheduled(fixedDelayString = "${market.stream.interval-ms:5000}")
    public void publishTrackedSymbols() {
        Instant cutoff = Instant.now().minus(SUBSCRIPTION_TTL);
        trackedSymbols.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));

        for (String trackedKey : trackedSymbols.keySet()) {
            String[] parts = trackedKey.split("\\|", 2);
            if (parts.length != 2) {
                continue;
            }

            String symbol = parts[0];
            InstrumentType type = InstrumentType.valueOf(parts[1]);
            publishSymbol(symbol, type);
        }
    }

    private void publishSymbol(String symbol, InstrumentType type) {
        try {
            MarketService.QuoteData quote = marketService.getQuoteByType(symbol, type);
            QuoteUpdateEvent event = new QuoteUpdateEvent(
                symbol,
                type,
                quote.symbol(),
                quote.price(),
                quote.bid(),
                quote.ask(),
                quote.change(),
                quote.changePercent(),
                quote.previousClose(),
                quote.currency(),
                quote.asOf(),
                quote.marketState()
            );
            messagingTemplate.convertAndSend(quoteTopic(symbol, type), event);
        } catch (RuntimeException ignored) {
            // We intentionally avoid failing the scheduler for one symbol.
        }
    }

    private String key(String symbol, InstrumentType type) {
        return symbol + "|" + type.name();
    }

    private String quoteTopic(String symbol, InstrumentType type) {
        return "/topic/quotes/" + symbol + "/" + type.name();
    }

    private String normalize(String symbol) {
        return symbol.trim().toUpperCase();
    }

    public long getPublishIntervalMs() {
        return publishIntervalMs;
    }
}
