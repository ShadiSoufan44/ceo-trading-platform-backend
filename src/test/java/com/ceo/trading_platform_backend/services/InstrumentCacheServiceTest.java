package com.ceo.trading_platform_backend.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.models.InstrumentCandle;
import com.ceo.trading_platform_backend.repositories.InstrumentCandleRepository;

@ExtendWith(MockitoExtension.class)
class InstrumentCacheServiceTest {

    private static final String SYMBOL = "AAPL";
    private static final InstrumentType TYPE = InstrumentType.EQUITY;

    // "now" is Monday 2026-01-05 15:00 UTC, so the last finished day is Sunday 01-04
    private static final Instant NOW = Instant.parse("2026-01-05T15:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 5);
    private static final LocalDate LAST_FINISHED_DAY = TODAY.minusDays(1);
    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final Instant EARLIER_TODAY = Instant.parse("2026-01-05T09:00:00Z");

    @Mock
    private InstrumentCandleRepository instrumentCandleRepository;

    @Mock
    private MarketService marketService;

    @Mock
    private InstrumentService instrumentService;

    private InstrumentCacheService instrumentCacheService;
    private Instrument instrument;

    @BeforeEach
    void setUp() {
        instrumentCacheService = new InstrumentCacheService(
            instrumentCandleRepository, marketService, instrumentService, Clock.fixed(NOW, ZoneOffset.UTC)
        );
        instrument = new Instrument(SYMBOL, TYPE, "Apple Inc.");
    }

    @Test
    void returnsCachedCandlesWithoutCallingMarketServiceWhenCacheIsUpToDate() {
        List<InstrumentCandle> cachedCandles = List.of(
            candle(FROM, "104", EARLIER_TODAY),
            candle(LocalDate.of(2026, 1, 2), "107", EARLIER_TODAY)
        );
        givenCached(cachedCandles);

        List<InstrumentCandle> result = instrumentCacheService.findOrInsertCandles(SYMBOL, TYPE, FROM, TODAY);

        assertThat(result).containsExactlyElementsOf(cachedCandles);
        verify(marketService, never()).getCandlesByType(any(), any(), any(), any());
        verify(instrumentCandleRepository, never()).saveAll(anyList());
    }

    @Test
    void fetchesAndPersistsFinishedCandlesButNeverTodays() {
        givenCached(List.of());
        when(marketService.getCandlesByType(SYMBOL, TYPE, FROM, LAST_FINISHED_DAY)).thenReturn(List.of(
            marketCandle(LocalDate.of(2026, 1, 2), "104"),
            marketCandle(TODAY, "999")
        ));
        givenSaveAllReturnsInput();

        List<InstrumentCandle> result = instrumentCacheService.findOrInsertCandles(SYMBOL, TYPE, FROM, TODAY);

        assertThat(result).hasSize(1);
        InstrumentCandle saved = result.get(0);
        assertThat(saved.getInstrument()).isSameAs(instrument);
        assertThat(saved.getCandleDate()).isEqualTo(utcStart(LocalDate.of(2026, 1, 2)));
        assertThat(saved.getClose()).isEqualByComparingTo("104");
        assertThat(saved.getVolume()).isEqualByComparingTo("1000");
        assertThat(saved.getFetchedAt()).isEqualTo(NOW);
    }

    @Test
    void overwritesCandleThatWasCachedBeforeItsDayEnded() {
        LocalDate day = LocalDate.of(2026, 1, 2);
        InstrumentCandle partial = candle(day, "100", Instant.parse("2026-01-02T15:00:00Z"));
        givenCached(List.of(candle(FROM, "98", EARLIER_TODAY), partial));
        when(marketService.getCandlesByType(SYMBOL, TYPE, FROM, LAST_FINISHED_DAY)).thenReturn(List.of(
            marketCandle(FROM, "98"),
            marketCandle(day, "104")
        ));
        givenSaveAllReturnsInput();

        List<InstrumentCandle> result = instrumentCacheService.findOrInsertCandles(SYMBOL, TYPE, FROM, TODAY);

        assertThat(result).hasSize(2);
        assertThat(result.get(1)).isSameAs(partial);
        assertThat(partial.getClose()).isEqualByComparingTo("104");
        assertThat(partial.getFetchedAt()).isEqualTo(NOW);
    }

    @Test
    void refetchesCandlesCachedBeforeFetchedAtWasTracked() {
        givenCached(List.of(candle(FROM, "98", null), candle(LocalDate.of(2026, 1, 2), "104", null)));
        when(marketService.getCandlesByType(SYMBOL, TYPE, FROM, LAST_FINISHED_DAY)).thenReturn(List.of());
        givenSaveAllReturnsInput();

        instrumentCacheService.findOrInsertCandles(SYMBOL, TYPE, FROM, TODAY);

        verify(marketService).getCandlesByType(SYMBOL, TYPE, FROM, LAST_FINISHED_DAY);
    }

    @Test
    void asksTheApiAgainOnceANewDayHasFinished() {
        // final candles, but last checked yesterday: a new session may have finished since
        Instant yesterday = Instant.parse("2026-01-04T10:00:00Z");
        givenCached(List.of(candle(FROM, "98", yesterday), candle(LocalDate.of(2026, 1, 2), "104", yesterday)));
        when(marketService.getCandlesByType(SYMBOL, TYPE, FROM, LAST_FINISHED_DAY)).thenReturn(List.of());
        givenSaveAllReturnsInput();

        instrumentCacheService.findOrInsertCandles(SYMBOL, TYPE, FROM, TODAY);

        verify(marketService).getCandlesByType(SYMBOL, TYPE, FROM, LAST_FINISHED_DAY);
    }

    @Test
    void refetchesWhenTheCacheDoesNotReachBackToTheStartOfTheRange() {
        LocalDate from = LocalDate.of(2025, 12, 1);
        when(instrumentService.getInstrumentBySymbolAndType(SYMBOL, TYPE)).thenReturn(instrument);
        when(instrumentCandleRepository.findCandleInDateRange(
            eq(instrument.getID()), eq(utcStart(from)), eq(utcStart(TODAY))
        )).thenReturn(List.of(candle(LocalDate.of(2026, 1, 2), "104", EARLIER_TODAY)));
        when(marketService.getCandlesByType(SYMBOL, TYPE, from, LAST_FINISHED_DAY)).thenReturn(List.of());
        givenSaveAllReturnsInput();

        instrumentCacheService.findOrInsertCandles(SYMBOL, TYPE, from, TODAY);

        verify(marketService).getCandlesByType(SYMBOL, TYPE, from, LAST_FINISHED_DAY);
    }

    @Test
    void returnsNothingForARangeWithNoFinishedDays() {
        List<InstrumentCandle> result = instrumentCacheService.findOrInsertCandles(SYMBOL, TYPE, TODAY, TODAY);

        assertThat(result).isEmpty();
        verifyNoInteractions(marketService, instrumentCandleRepository);
    }

    @Test
    void returnsCloseForCandleOnDate() {
        InstrumentCandle candle = candle(FROM, "104", EARLIER_TODAY);
        when(instrumentService.getOrInsertInstrumentBySymbolAndType(SYMBOL, TYPE))
            .thenReturn(instrument);
        when(instrumentCandleRepository.findByInstrumentIDAndCandleDate(
            eq(instrument.getID()), eq(utcStart(FROM))
        )).thenReturn(List.of(candle));

        BigDecimal result = instrumentCacheService.getPriceWithTypeAtDate(SYMBOL, TYPE, FROM);

        assertThat(result).isEqualByComparingTo("104");
    }

    private void givenCached(List<InstrumentCandle> candles) {
        when(instrumentService.getInstrumentBySymbolAndType(SYMBOL, TYPE)).thenReturn(instrument);
        when(instrumentCandleRepository.findCandleInDateRange(
            eq(instrument.getID()), eq(utcStart(FROM)), eq(utcStart(TODAY))
        )).thenReturn(candles);
    }

    private void givenSaveAllReturnsInput() {
        when(instrumentCandleRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private InstrumentCandle candle(LocalDate date, String close, Instant fetchedAt) {
        BigDecimal price = new BigDecimal(close);
        InstrumentCandle candle = new InstrumentCandle(
            instrument, utcStart(date), price, price, price, price, null, BigDecimal.valueOf(1000), false
        );
        candle.setFetchedAt(fetchedAt);
        return candle;
    }

    private MarketService.Candle marketCandle(LocalDate date, String close) {
        BigDecimal price = new BigDecimal(close);
        return new MarketService.Candle(date, price, price, price, price, null, 1000L, false);
    }

    private Instant utcStart(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
