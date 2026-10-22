package com.ceo.trading_platform_backend.controllers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ceo.trading_platform_backend.dto.ClientActivityTrendPoint;
import com.ceo.trading_platform_backend.dto.TopInstrumentInsight;
import com.ceo.trading_platform_backend.dto.TopMover;
import com.ceo.trading_platform_backend.dto.TradingVolumeInsight;
import com.ceo.trading_platform_backend.services.AnalystService;


@RestController
@RequestMapping("/api/analytics")
public class AnalystController {

    private final AnalystService analystService;

    public AnalystController(AnalystService analystService) {
        this.analystService = analystService;
    }

    @GetMapping("/volume")
    public ResponseEntity<TradingVolumeInsight> getTradingVolume(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String clientSegment) {
        Instant fromUtc = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusiveUtc = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return ResponseEntity.ok(analystService.getTradingVolume(fromUtc, toExclusiveUtc, symbol, clientSegment));
    }

    @GetMapping("/top-instruments")
    public ResponseEntity<List<TopInstrumentInsight>> getTopInstruments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "5") int limit) {
        Instant fromUtc = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusiveUtc = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return ResponseEntity.ok(analystService.getTopInstruments(fromUtc, toExclusiveUtc, limit));
    }

    @GetMapping("/top-movers")
    public ResponseEntity<List<TopMover>> getTopMovers(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(analystService.getTopMovers(limit));
    }

    @GetMapping("/client-activity-trends")
    public ResponseEntity<List<ClientActivityTrendPoint>> getClientActivityTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String clientSegment) {
        Instant fromUtc = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusiveUtc = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return ResponseEntity.ok(analystService.getClientActivityTrend(fromUtc, toExclusiveUtc, clientSegment));
    }

    @GetMapping("/candles/{symbol}")
    public ResponseEntity<Object> getCandles(
            @PathVariable String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analystService.getHistoricalData(symbol, from, to));
    }
}
