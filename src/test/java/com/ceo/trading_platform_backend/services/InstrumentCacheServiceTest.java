package com.ceo.trading_platform_backend.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

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
    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 1, 3);

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
            instrumentCandleRepository, marketService, instrumentService
        );
        instrument = new Instrument(SYMBOL, TYPE, "Apple Inc.");

    }

    @Test
    void returnsCachedCandlesWithoutCallingMarketServiceWhenCacheIsFull() {
        when(instrumentService.getInstrumentBySymbolAndType(SYMBOL, TYPE))
            .thenReturn(instrument);
        List<InstrumentCandle> cachedCandles = List.of(
            candle(FROM, "100", "105", "99", "104"),
            candle(FROM.plusDays(1), "104", "108", "103", "107"),
            candle(TO, "107", "110", "106", "109")
        );
        when(instrumentCandleRepository.findCandleInDateRange(
            eq(instrument.getID()), eq(utcStart(FROM)), eq(utcStart(TO.plusDays(1)))
        )).thenReturn(cachedCandles);

        List<InstrumentCandle> result = instrumentCacheService.findOrInsertCandles(
            SYMBOL, TYPE, FROM, TO
        );

        assertThat(result).containsExactlyElementsOf(cachedCandles);
        verify(marketService, never()).getCandlesByType(any(), any(), any(), any());
        verify(instrumentCandleRepository, never()).save(any());
    }

    @Test
    void fetchesAndPersistsCandlesWhenCacheIsIncomplete() {
        when(instrumentService.getInstrumentBySymbolAndType(SYMBOL, TYPE))
            .thenReturn(instrument);
        MarketService.Candle marketCandle = marketCandle(FROM, "100", "105", "99", "104");
        when(instrumentCandleRepository.findCandleInDateRange(
            eq(instrument.getID()), eq(utcStart(FROM)), eq(utcStart(TO.plusDays(1)))
        )).thenReturn(List.of());
        when(marketService.getCandlesByType(eq(SYMBOL), eq(TYPE), eq(FROM), eq(TO)))
            .thenReturn(List.of(marketCandle));
        when(instrumentCandleRepository.findByInstrumentIDAndCandleDate(
            eq(instrument.getID()), eq(utcStart(FROM))
        )).thenReturn(List.of());
        when(instrumentCandleRepository.save(any(InstrumentCandle.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        List<InstrumentCandle> result = instrumentCacheService.findOrInsertCandles(
            SYMBOL, TYPE, FROM, TO
        );

        assertThat(result).hasSize(1);
        InstrumentCandle savedCandle = result.get(0);
        assertThat(savedCandle.getInstrument()).isSameAs(instrument);
        assertThat(savedCandle.getCandleDate()).isEqualTo(utcStart(FROM));
        assertThat(savedCandle.getOpen()).isEqualByComparingTo("100");
        assertThat(savedCandle.getHigh()).isEqualByComparingTo("105");
        assertThat(savedCandle.getLow()).isEqualByComparingTo("99");
        assertThat(savedCandle.getClose()).isEqualByComparingTo("104");
        assertThat(savedCandle.getVolume()).isEqualByComparingTo("1000");
        assertThat(savedCandle.getSynthetic()).isFalse();
        verify(instrumentCandleRepository).save(savedCandle);
    }

    @Test
    void reusesExistingCandleWhenMarketDataAlreadyExistsForDate() {
        when(instrumentService.getInstrumentBySymbolAndType(SYMBOL, TYPE))
            .thenReturn(instrument);
        InstrumentCandle existingCandle = candle(FROM, "100", "105", "99", "104");
        when(instrumentCandleRepository.findCandleInDateRange(
            eq(instrument.getID()), eq(utcStart(FROM)), eq(utcStart(TO.plusDays(1)))
        )).thenReturn(List.of());
        when(marketService.getCandlesByType(eq(SYMBOL), eq(TYPE), eq(FROM), eq(TO)))
            .thenReturn(List.of(marketCandle(FROM, "90", "95", "89", "94")));
        when(instrumentCandleRepository.findByInstrumentIDAndCandleDate(
            eq(instrument.getID()), eq(utcStart(FROM))
        )).thenReturn(List.of(existingCandle));

        List<InstrumentCandle> result = instrumentCacheService.findOrInsertCandles(
            SYMBOL, TYPE, FROM, TO
        );

        assertThat(result).containsExactly(existingCandle);
        verify(instrumentCandleRepository, never()).save(any());
    }

    @Test
    void returnsCloseForCandleOnDate() {
        InstrumentCandle candle = candle(FROM, "100", "105", "99", "104");
        when(instrumentService.getOrInsertInstrumentBySymbolAndType(SYMBOL, TYPE))
            .thenReturn(instrument);
        when(instrumentCandleRepository.findByInstrumentIDAndCandleDate(
            eq(instrument.getID()), eq(utcStart(FROM))
        )).thenReturn(List.of(candle));

        BigDecimal result = instrumentCacheService.getPriceWithTypeAtDate(SYMBOL, TYPE, FROM);

        assertThat(result).isEqualByComparingTo("104");
    }

    private InstrumentCandle candle(
        LocalDate date, String open, String high, String low, String close
    ) {
        return new InstrumentCandle(
            instrument,
            utcStart(date),
            new BigDecimal(open),
            new BigDecimal(high),
            new BigDecimal(low),
            new BigDecimal(close),
            null,
            BigDecimal.valueOf(1000),
            false
        );
    }

    private MarketService.Candle marketCandle(
        LocalDate date, String open, String high, String low, String close
    ) {
        return new MarketService.Candle(
            date,
            new BigDecimal(open),
            new BigDecimal(high),
            new BigDecimal(low),
            new BigDecimal(close),
            null,
            1000L,
            false
        );
    }

    private Instant utcStart(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
