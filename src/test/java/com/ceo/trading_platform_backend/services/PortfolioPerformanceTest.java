package com.ceo.trading_platform_backend.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

import com.ceo.trading_platform_backend.dto.PerformanceResponse;
import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.enums.PortfolioType;
import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.models.InstrumentCandle;
import com.ceo.trading_platform_backend.models.Portfolio;
import com.ceo.trading_platform_backend.repositories.PortfolioRepository;

@ExtendWith(MockitoExtension.class)
class PortfolioPerformanceTest {

    @Mock private PortfolioRepository repository;
    @Mock private InstrumentService instrumentService;
    @Mock private InstrumentCacheService cache;
    @Mock private MarketService marketService;
    @Mock private UserService userService;

    private PortfolioService service;
    private final UUID portfolioId = UUID.randomUUID();
    private final LocalDate end = LocalDate.now();
    private final LocalDate start = end.minusWeeks(1);

    @BeforeEach
    void setUp() {
        service = new PortfolioService(repository, instrumentService, cache, marketService, userService);
    }

    @Test
    void consolidatesPositionsAndLoadsEachInstrumentOnlyOnce() {
        Portfolio portfolio = portfolio();
        Instrument apple = instrument("AAPL", InstrumentType.EQUITY);
        // Separate instances of the same instrument must also be consolidated.
        addHolding(portfolio, apple, "3");
        addHolding(portfolio, instrument("AAPL", InstrumentType.EQUITY), "-1");
        addHolding(portfolio, instrument("USD", InstrumentType.CASH), "100");
        Instrument bitcoin = instrument("BTC", InstrumentType.CRYPTO);
        addHolding(portfolio, bitcoin, "1");
        when(cache.findOrInsertCandles("AAPL", InstrumentType.EQUITY, start, end))
            .thenReturn(List.of(candle(apple, end, "12"), candle(apple, start, "10")));
        when(cache.findOrInsertCandles("BTC", InstrumentType.CRYPTO, start, end))
            .thenReturn(List.of(candle(bitcoin, start, "20")));

        List<PerformanceResponse> result = service.getPortfolioPerformance(portfolioId, "1W");

        assertThat(result).hasSize(8);
        assertThat(result).extracting(PerformanceResponse::date).isSorted().doesNotHaveDuplicates();
        assertThat(result.getFirst().date()).isEqualTo(timestamp(start));
        assertThat(result.getFirst().balance()).isEqualByComparingTo("140");
        assertThat(result.getLast().balance()).isEqualByComparingTo("144");
        verify(repository).getReferenceById(portfolioId);
        verify(cache).findOrInsertCandles("AAPL", InstrumentType.EQUITY, start, end);
        verify(cache).findOrInsertCandles("BTC", InstrumentType.CRYPTO, start, end);
        // In particular, no getPriceWithTypeAtDate() calls and no market API calls.
        verifyNoMoreInteractions(repository, cache);
        verifyNoInteractions(instrumentService, marketService);
    }

    @Test
    void carriesPricesForwardWithoutUsingFuturePrices() {
        Portfolio portfolio = portfolio();
        Instrument apple = instrument("AAPL", InstrumentType.EQUITY);
        addHolding(portfolio, apple, "2");
        when(cache.findOrInsertCandles("AAPL", InstrumentType.EQUITY, start, end))
            .thenReturn(List.of(candle(apple, start, "10"), candle(apple, start.plusDays(3), "15")));

        List<PerformanceResponse> result = service.getPortfolioPerformance(portfolioId, "1W");

        assertThat(result).hasSize(8);
        assertThat(result.get(2).balance()).isEqualByComparingTo("20");
        assertThat(result.get(3).balance()).isEqualByComparingTo("30");
        assertThat(result.getLast().balance()).isEqualByComparingTo("30");
    }

    @Test
    void omitsInitialDatesUntilEveryPositionHasAKnownPrice() {
        Portfolio portfolio = portfolio();
        Instrument apple = instrument("AAPL", InstrumentType.EQUITY);
        Instrument microsoft = instrument("MSFT", InstrumentType.EQUITY);
        addHolding(portfolio, apple, "1");
        addHolding(portfolio, microsoft, "1");
        when(cache.findOrInsertCandles("AAPL", InstrumentType.EQUITY, start, end))
            .thenReturn(List.of(candle(apple, start, "10")));
        when(cache.findOrInsertCandles("MSFT", InstrumentType.EQUITY, start, end))
            .thenReturn(List.of(candle(microsoft, start.plusDays(2), "20")));

        List<PerformanceResponse> result = service.getPortfolioPerformance(portfolioId, "1W");

        assertThat(result).hasSize(6);
        assertThat(result.getFirst().date()).isEqualTo(timestamp(start.plusDays(2)));
        assertThat(result).allSatisfy(point -> assertThat(point.balance()).isEqualByComparingTo("30"));
    }

    @Test
    void returnsNoInventedBalancesWhenAnInstrumentHasNoPrices() {
        Portfolio portfolio = portfolio();
        addHolding(portfolio, instrument("AAPL", InstrumentType.EQUITY), "1");
        when(cache.findOrInsertCandles("AAPL", InstrumentType.EQUITY, start, end)).thenReturn(List.of());

        assertThat(service.getPortfolioPerformance(portfolioId, "1W")).isEmpty();
    }

    @Test
    void valuesCashAndSkipsZeroNetPositionsWithoutFetchingCandles() {
        Portfolio portfolio = portfolio();
        addHolding(portfolio, instrument("USD", InstrumentType.CASH), "100");
        Instrument apple = instrument("AAPL", InstrumentType.EQUITY);
        addHolding(portfolio, apple, "2");
        addHolding(portfolio, apple, "-2");

        List<PerformanceResponse> result = service.getPortfolioPerformance(portfolioId, "1M");

        assertThat(result).hasSize((int) end.minusMonths(1).datesUntil(end.plusDays(1)).count());
        assertThat(result).allSatisfy(point -> assertThat(point.balance()).isEqualByComparingTo("100"));
        verifyNoInteractions(cache, marketService, instrumentService);
    }

    @Test
    void returnsEmptyForAnEmptyPortfolio() {
        portfolio();

        assertThat(service.getPortfolioPerformance(portfolioId, "1W")).isEmpty();
        verifyNoInteractions(cache, marketService, instrumentService);
    }

    @Test
    void rejectsInvalidTimeframesBeforeLoadingData() {
        assertThatThrownBy(() -> service.getPortfolioPerformance(portfolioId, "invalid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid timeframe");
        verifyNoInteractions(repository, cache, marketService, instrumentService);
    }

    private Portfolio portfolio() {
        Portfolio portfolio = new Portfolio(PortfolioType.values()[0]);
        when(repository.getReferenceById(portfolioId)).thenReturn(portfolio);
        return portfolio;
    }

    private Instrument instrument(String symbol, InstrumentType type) {
        return new Instrument(symbol, type, symbol);
    }

    private void addHolding(Portfolio portfolio, Instrument instrument, String quantity) {
        portfolio.addHolding(new Holding(
            Instant.now(), BigDecimal.ONE, new BigDecimal(quantity), instrument, null, portfolio
        ));
    }

    private InstrumentCandle candle(Instrument instrument, LocalDate date, String price) {
        BigDecimal close = new BigDecimal(price);
        return new InstrumentCandle(
            instrument, date.atStartOfDay(ZoneOffset.UTC).toInstant(),
            close, close, close, close, close, BigDecimal.ZERO, false
        );
    }

    private String timestamp(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toString();
    }
}