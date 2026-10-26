package com.ceo.trading_platform_backend.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.ceo.trading_platform_backend.services.RealMarketService.MarketClientException;
import com.ceo.trading_platform_backend.services.RealMarketService.MarketServerException;
import com.ceo.trading_platform_backend.services.RealMarketService.MarketSymbolNotFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsMarketRateLimitToTooManyRequests() {
        var response = handler.handleMarketClientError(new MarketClientException(429, "quota reached"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).containsEntry("status", 429);
    }

    @Test
    void mapsUnknownMarketSymbolToNotFound() {
        var response = handler.handleMarketSymbolNotFound(new MarketSymbolNotFoundException("Unknown symbol"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("status", 404);
    }

    @Test
    void mapsMarketServerFailureToBadGateway() {
        var response = handler.handleMarketServerError(new MarketServerException(503, "provider unavailable"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).containsEntry("status", 502);
    }
}