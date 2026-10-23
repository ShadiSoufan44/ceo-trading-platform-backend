package com.ceo.trading_platform_backend.services;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ceo.trading_platform_backend.dto.ClientActivityTrendPoint;
import com.ceo.trading_platform_backend.dto.TopInstrumentInsight;
import com.ceo.trading_platform_backend.dto.TopMover;
import com.ceo.trading_platform_backend.dto.TradingVolumeInsight;
import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.repositories.AnalystReportingRepository;
import com.ceo.trading_platform_backend.repositories.InstrumentRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalystService Tests")
class AnalystServiceTest {

    @Mock
    private MarketService marketService;

    @Mock
    private AnalystReportingRepository reportingRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    private AnalystService analystService;

    @BeforeEach
    void setUp() {
        analystService = new AnalystService(marketService, reportingRepository, instrumentRepository);
    }

    @Test
    @DisplayName("getTradingVolume returns mapped aggregate values")
    void getTradingVolumeReturnsMappedAggregateValues() {
        Instant from = LocalDate.of(2026, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = LocalDate.of(2026, 2, 1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Object[]> volumeRows = new ArrayList<>();
        volumeRows.add(new Object[] { new BigDecimal("1000.50"), new BigDecimal("12.5"), Long.valueOf(7) });
        when(reportingRepository.getTradingVolume(from, to, "AAPL", "RETAIL"))
            .thenReturn(volumeRows);

        TradingVolumeInsight insight = analystService.getTradingVolume(from, to, "aapl", "retail");

        assertThat(insight.totalNotional()).isEqualByComparingTo("1000.50");
        assertThat(insight.totalQuantity()).isEqualByComparingTo("12.5");
        assertThat(insight.executedTrades()).isEqualTo(7L);
        assertThat(insight.symbol()).isEqualTo("AAPL");
        assertThat(insight.clientSegment()).isEqualTo("RETAIL");
    }

    @Test
    @DisplayName("getTradingVolume returns zero metrics when reporting data is empty")
    void getTradingVolumeReturnsZeroMetricsWhenReportingDataIsEmpty() {
        Instant from = LocalDate.of(2026, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = LocalDate.of(2026, 2, 1).atStartOfDay().toInstant(ZoneOffset.UTC);

        when(reportingRepository.getTradingVolume(from, to, null, null)).thenReturn(List.of());

        TradingVolumeInsight insight = analystService.getTradingVolume(from, to, null, null);

        assertThat(insight.totalNotional()).isZero();
        assertThat(insight.totalQuantity()).isZero();
        assertThat(insight.executedTrades()).isZero();
    }

    @Test
    @DisplayName("getTopInstruments returns ranked instrument insights")
    void getTopInstrumentsReturnsRankedInstrumentInsights() {
        Instant from = LocalDate.of(2026, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = LocalDate.of(2026, 2, 1).atStartOfDay().toInstant(ZoneOffset.UTC);

        when(reportingRepository.getTopInstruments(from, to, 2)).thenReturn(
                List.of(
                        new Object[] { "AAPL", new BigDecimal("1200"), new BigDecimal("50"), Integer.valueOf(10) },
                        new Object[] { "MSFT", new BigDecimal("900"), new BigDecimal("40"), Integer.valueOf(8) }));

        List<TopInstrumentInsight> insights = analystService.getTopInstruments(from, to, 2);

        assertThat(insights).hasSize(2);
        assertThat(insights.get(0).symbol()).isEqualTo("AAPL");
        assertThat(insights.get(0).executedTrades()).isEqualTo(10L);
    }

        @Test
        @DisplayName("getTopMovers ranks quotes by absolute percentage change")
        void getTopMoversRanksQuotesByAbsolutePercentageChange() {
        when(instrumentRepository.findByType(InstrumentType.EQUITY)).thenReturn(List.of(
            new Instrument("AAPL", InstrumentType.EQUITY, "Apple"),
            new Instrument("TSLA", InstrumentType.EQUITY, "Tesla")));
        when(marketService.getQuotes(List.of("AAPL", "TSLA"))).thenReturn(List.of(
            new MarketService.BatchQuoteResult("AAPL", "market", false,
                new MarketService.QuoteData("AAPL", new BigDecimal("200"), null, null, null, "USD",
                    null, new BigDecimal("-2.5"), new BigDecimal("205"), null, null),
                null),
            new MarketService.BatchQuoteResult("TSLA", "market", false,
                new MarketService.QuoteData("TSLA", new BigDecimal("300"), null, null, null, "USD",
                    null, new BigDecimal("4.2"), new BigDecimal("288"), null, null),
                null)));

        List<TopMover> movers = analystService.getTopMovers(5);

        assertThat(movers).extracting(TopMover::symbol).containsExactly("TSLA", "AAPL");
        assertThat(movers.get(0).percentageChange()).isEqualByComparingTo("4.2");
        }

    @Test
    @DisplayName("getClientActivityTrend returns daily trend points")
    void getClientActivityTrendReturnsDailyTrendPoints() {
        Instant from = LocalDate.of(2026, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = LocalDate.of(2026, 2, 1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Timestamp period = Timestamp.from(Instant.parse("2026-01-03T00:00:00Z"));
        List<Object[]> trendRows = new ArrayList<>();
        trendRows.add(new Object[] { period, "RETAIL", new BigDecimal("500"), new BigDecimal("22"), Long.valueOf(5) });
        when(reportingRepository.getClientActivityTrend(from, to, "RETAIL")).thenReturn(trendRows);

        List<ClientActivityTrendPoint> points = analystService.getClientActivityTrend(from, to, "retail");

        assertThat(points).hasSize(1);
        assertThat(points.get(0).periodStart()).isEqualTo(Instant.parse("2026-01-03T00:00:00Z"));
        assertThat(points.get(0).clientSegment()).isEqualTo("RETAIL");
        assertThat(points.get(0).executedTrades()).isEqualTo(5L);
    }
}
