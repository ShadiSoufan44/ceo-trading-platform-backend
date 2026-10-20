package com.ceo.trading_platform_backend.services;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.ceo.trading_platform_backend.enums.InstrumentType;

@ExtendWith(MockitoExtension.class)
class QuoteStreamingServiceTest {

    @Mock
    private MarketService marketService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    

    @Test
    void trackSymbolShouldPublishImmediatelyAndOnScheduledTick() {
        QuoteStreamingService quoteStreamingService = new QuoteStreamingService(marketService, messagingTemplate, 5000);

        when(marketService.getQuoteByType("AAPL", InstrumentType.EQUITY)).thenReturn(
            new MarketService.QuoteData(
                "AAPL",
                BigDecimal.valueOf(200),
                BigDecimal.valueOf(199.9),
                BigDecimal.valueOf(200.1),
                BigDecimal.ZERO,
                "USD",
                BigDecimal.ONE,
                BigDecimal.valueOf(0.5),
                BigDecimal.valueOf(199),
                Instant.now(),
                "OPEN"
            )
        );

        quoteStreamingService.trackSymbol("aapl", InstrumentType.EQUITY);
        quoteStreamingService.publishTrackedSymbols();

        verify(messagingTemplate, atLeastOnce()).convertAndSend(
            org.mockito.ArgumentMatchers.eq("/topic/quotes/AAPL/EQUITY"),
            org.mockito.ArgumentMatchers.any(Object.class)
        );
    }
}
