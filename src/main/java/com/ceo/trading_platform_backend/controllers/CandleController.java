package com.ceo.trading_platform_backend.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.services.InstrumentCacheService;
import com.ceo.trading_platform_backend.services.InstrumentService;
import com.ceo.trading_platform_backend.services.InstrumentPerformanceService;
import com.ceo.trading_platform_backend.services.MarketService;

import com.ceo.trading_platform_backend.dto.PerformanceResponse;

/**
 * CandleController handles candle requests.
 */
@RestController
@RequestMapping("/api/candles/{symbol}/{type}")
public class CandleController {

    private final MarketService marketService;
    private final InstrumentCacheService candleService;
    private final InstrumentService instrumentService;
    private final InstrumentPerformanceService instrumentPerformanceService;

    public CandleController(MarketService marketService, InstrumentCacheService candleService, InstrumentService instrumentService, InstrumentPerformanceService instrumentPerformanceService) {
        this.marketService = marketService;
        this.candleService = candleService;
        this.instrumentService = instrumentService;
        this.instrumentPerformanceService = instrumentPerformanceService;
    }

    /**
     * Get stcok info for frontend stock display page timeframe not including today
     */
    @GetMapping("/{timeframe}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public ResponseEntity<Object> getInstrumentPerformance(
        @PathVariable String symbol,
        @PathVariable InstrumentType type,
        @PathVariable String timeframe
    ) {
        // LocalDate end = LocalDate.now(); // maybe this shoudl go in servuce...?
        // LocalDate start = candleService.calculateStartDate(timeframe, end);
        // Object candle = candleService.findOrInsertCandles(symbol, type, start, end);
        // return ResponseEntity.ok(candle);
        Instrument instrument = instrumentService.getOrInsertInstrumentBySymbolAndType(symbol, type);
        List<PerformanceResponse> performance = instrumentPerformanceService.getInstrumentPerformance(instrument.getID(), timeframe);
        return ResponseEntity.ok(performance);
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public ResponseEntity<Object> getTodaysCandle(
        @PathVariable String symbol,
        @PathVariable InstrumentType type
    ) {
        Instrument instrument = instrumentService.getOrInsertInstrumentBySymbolAndType(symbol, type);
        Object candle = candleService.getTodaysCandle(instrument.getID());
        return ResponseEntity.ok(candle);
    }
}
