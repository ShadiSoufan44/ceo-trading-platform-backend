package com.ceo.trading_platform_backend.services;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ceo.trading_platform_backend.dto.ClientActivityTrendPoint;
import com.ceo.trading_platform_backend.dto.TopInstrumentInsight;
import com.ceo.trading_platform_backend.dto.TradingVolumeInsight;
import com.ceo.trading_platform_backend.repositories.AnalystReportingRepository;

/**
 * AnalystService handles analyst-specific reporting and market data operations.
 */
@Service
public class AnalystService {

    private static final String DEFAULT_FILTER_VALUE = "ALL";

    private final MarketService marketService;
    private final AnalystReportingRepository reportingRepository;

    public AnalystService(MarketService marketService, AnalystReportingRepository reportingRepository) {
        this.marketService = marketService;
        this.reportingRepository = reportingRepository;
    }

    /**
     * Get historical price data for a symbol.
     */
    @Transactional(readOnly = true)
    public List<MarketService.Candle> getHistoricalData(String symbol) {
        return marketService.getCandles(symbol, null, null);
    }

    /**
     * Get historical price data for a symbol with an optional date range.
     */
    @Transactional(readOnly = true)
    public List<MarketService.Candle> getHistoricalData(String symbol, LocalDate from, LocalDate to) {
        return marketService.getCandles(symbol, from, to);
    }

    /**
     * Aggregate trading volume metrics by optional instrument and client segment filters.
     */
    @Transactional(readOnly = true)
    public TradingVolumeInsight getTradingVolume(Instant from, Instant to, String symbol, String clientSegment) {
        String normalizedSymbol = normalizeFilter(symbol);
        String normalizedSegment = normalizeFilter(clientSegment);
        List<Object[]> results = reportingRepository.getTradingVolume(from, to, normalizedSymbol, normalizedSegment);
        Object[] result = results.isEmpty()
            ? new Object[] { BigDecimal.ZERO, BigDecimal.ZERO, 0L }
            : results.getFirst();

        BigDecimal totalNotional = (BigDecimal) result[0];
        BigDecimal totalQuantity = (BigDecimal) result[1];
        long executedTrades = ((Number) result[2]).longValue();

        return new TradingVolumeInsight(from, to, defaultIfMissing(normalizedSymbol), defaultIfMissing(normalizedSegment),
                totalNotional, totalQuantity, executedTrades);
    }

    /**
     * Return most active instruments ranked by notional traded.
     */
    @Transactional(readOnly = true)
    public List<TopInstrumentInsight> getTopInstruments(Instant from, Instant to, int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 100));
        return reportingRepository.getTopInstruments(from, to, cappedLimit)
                .stream()
                .map(this::toTopInstrumentInsight)
                .toList();
    }

    /**
     * Return per-day client activity trend points.
     */
    @Transactional(readOnly = true)
    public List<ClientActivityTrendPoint> getClientActivityTrend(Instant from, Instant to, String clientSegment) {
        String normalizedSegment = normalizeFilter(clientSegment);
        return reportingRepository.getClientActivityTrend(from, to, normalizedSegment)
                .stream()
                .map(this::toClientActivityTrendPoint)
                .toList();
    }

    private TopInstrumentInsight toTopInstrumentInsight(Object[] row) {
        return new TopInstrumentInsight(
                (String) row[0],
                (BigDecimal) row[1],
                (BigDecimal) row[2],
                ((Number) row[3]).longValue());
    }

    private ClientActivityTrendPoint toClientActivityTrendPoint(Object[] row) {
        return new ClientActivityTrendPoint(
                toInstant(row[0]),
                (String) row[1],
                (BigDecimal) row[2],
                (BigDecimal) row[3],
                ((Number) row[4]).longValue());
    }

    private Instant toInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        throw new IllegalArgumentException("Unsupported timestamp type in analytics result set: " + value);
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase();
    }

    private String defaultIfMissing(String value) {
        return value == null ? DEFAULT_FILTER_VALUE : value;
    }
}
