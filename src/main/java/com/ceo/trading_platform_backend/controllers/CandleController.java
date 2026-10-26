package com.ceo.trading_platform_backend.controllers;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.services.InstrumentCacheService;
import com.ceo.trading_platform_backend.services.MarketService;

/**
 * CandleController handles candle requests.
 */
@RestController
@RequestMapping("/api/candle")
public class CandleController {

    private final MarketService marketService;
    private final InstrumentCacheService candleService;

    public CandleController(MarketService marketService, InstrumentCacheService candleService) {
        this.marketService = marketService;
        this.candleService = candleService;
    }

    /**
     * Get current candle for a symbol.
     * Available to all authenticated users.
     */
    @GetMapping("/{symbol}/{type}/{timeframe}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public ResponseEntity<Object> getCandle(
        @PathVariable String symbol,
        @PathVariable InstrumentType type,
        @PathVariable String timeframe
    ) {
        LocalDate end = LocalDate.now(); // maybe this shoudl go in servuce...?
        LocalDate start = candleService.calculateStartDate(timeframe, end);
        Object candle = candleService.findOrInsertCandles(symbol, type, start, end);
        return ResponseEntity.ok(candle);
    }
}
