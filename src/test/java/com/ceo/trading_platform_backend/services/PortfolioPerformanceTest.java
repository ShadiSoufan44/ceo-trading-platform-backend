package com.ceo.trading_platform_backend.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
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
    private final LocalDate today = LocalDate.of(2026, 10, 27);
    private final LocalDate end = today.minusDays(1);
    private final LocalDate start = today.minusWeeks(1);
    private final LocalDate priceStart = start.minusDays(7);

    @BeforeEach
    void setUp() {
        service = new PortfolioService(repository, instrumentService, cache, marketService, userService,
            Clock.fixed(today.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
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
        when(cache.findOrInsertCandles("AAPL", InstrumentType.EQUITY, priceStart, end))
            .thenReturn(List.of(candle(apple, end, "12"), candle(apple, start, "10")));
        when(cache.findOrInsertCandles("BTC", InstrumentType.CRYPTO, priceStart, end))
            .thenReturn(List.of(candle(bitcoin, start, "20")));

        List<PerformanceResponse> result = service.getPortfolioPerformance(portfolioId, "1W");

        assertThat(result).hasSize(7);
        assertThat(result).extracting(PerformanceResponse::date).isSorted().doesNotHaveDuplicates();
        assertThat(result.getFirst().date()).isEqualTo(timestamp(start));
        assertThat(result.getFirst().balance()).isEqualByComparingTo("140");
        assertThat(result.getLast().balance()).isEqualByComparingTo("144");
        verify(repository).getReferenceById(portfolioId);
        verify(cache).findOrInsertCandles("AAPL", InstrumentType.EQUITY, priceStart, end);
        verify(cache).findOrInsertCandles("BTC", InstrumentType.CRYPTO, priceStart, end);
        // In particular, no getPriceWithTypeAtDate() calls and no market API calls.
        verifyNoMoreInteractions(repository, cache);
        verifyNoInteractions(instrumentService, marketService);
    }

    @Test
    void carriesPricesForwardWithoutUsingFuturePrices() {
        Portfolio portfolio = portfolio();
        Instrument apple = instrument("AAPL", InstrumentType.EQUITY);
        addHolding(portfolio, apple, "2");
        when(cache.findOrInsertCandles("AAPL", InstrumentType.EQUITY, priceStart, end))
            .thenReturn(List.of(candle(apple, start, "10"), candle(apple, start.plusDays(3), "15")));

        List<PerformanceResponse> result = service.getPortfolioPerformance(portfolioId, "1W");

        assertThat(result).hasSize(7);
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
        when(cache.findOrInsertCandles("AAPL", InstrumentType.EQUITY, priceStart, end))
            .thenReturn(List.of(candle(apple, start, "10")));
        when(cache.findOrInsertCandles("MSFT", InstrumentType.EQUITY, priceStart, end))
            .thenReturn(List.of(candle(microsoft, start.plusDays(2), "20")));

        List<PerformanceResponse> result = service.getPortfolioPerformance(portfolioId, "1W");

        assertThat(result).hasSize(5);
        assertThat(result.getFirst().date()).isEqualTo(timestamp(start.plusDays(2)));
        assertThat(result).allSatisfy(point -> assertThat(point.balance()).isEqualByComparingTo("30"));
    }

    @Test
    void returnsNoInventedBalancesWhenAnInstrumentHasNoPrices() {
        Portfolio portfolio = portfolio();
        addHolding(portfolio, instrument("AAPL", InstrumentType.EQUITY), "1");
        when(cache.findOrInsertCandles("AAPL", InstrumentType.EQUITY, priceStart, end)).thenReturn(List.of());

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

        assertThat(result).hasSize((int) today.minusMonths(1).datesUntil(today).count());
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

    @Test
    void replaysUnorderedBuysSalesAndDepositsOnTheirActualDays() {
        Portfolio portfolio = portfolio();
        Instrument usd = instrument("USD", InstrumentType.CASH);
        Instrument apple = instrument("AAPL", InstrumentType.EQUITY);
        // Deliberately insert the sale first. The position is zero today, but was held in the past.
        addHolding(portfolio, apple, "-2", start.plusDays(4).atTime(15, 0).toInstant(ZoneOffset.UTC));
        addHolding(portfolio, usd, "30", start.plusDays(4).atTime(15, 0).toInstant(ZoneOffset.UTC));
        addHolding(portfolio, usd, "100");
        addHolding(portfolio, apple, "2", start.plusDays(2).atTime(12, 0).toInstant(ZoneOffset.UTC));
        addHolding(portfolio, usd, "-20", start.plusDays(2).atTime(12, 0).toInstant(ZoneOffset.UTC));
        addHolding(portfolio, usd, "50", start.plusDays(5).atTime(18, 0).toInstant(ZoneOffset.UTC));
        when(cache.findOrInsertCandles("AAPL", InstrumentType.EQUITY, priceStart, end))
            .thenReturn(List.of(candle(apple, start.plusDays(2), "10"),
                candle(apple, start.plusDays(3), "12")));

        List<PerformanceResponse> result = service.getPortfolioPerformance(portfolioId, "1W");

        assertThat(result).extracting(PerformanceResponse::balance).containsExactly(
            new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"),
            new BigDecimal("104"), new BigDecimal("110"), new BigDecimal("160"), new BigDecimal("160"));
        verify(cache).findOrInsertCandles("AAPL", InstrumentType.EQUITY, priceStart, end);
        verifyNoMoreInteractions(cache);
        verifyNoInteractions(marketService, instrumentService);
    }

    @Test
    void reconstructsOpeningPositionsAndDoesNotPricePositionsClosedBeforeRange() {
        Portfolio portfolio = portfolio();
        Instrument apple = instrument("AAPL", InstrumentType.EQUITY);
        addHolding(portfolio, apple, "2", start.minusDays(10).atStartOfDay(ZoneOffset.UTC).toInstant());
        addHolding(portfolio, apple, "-2", start.minusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant());
        addHolding(portfolio, instrument("USD", InstrumentType.CASH), "125");

        List<PerformanceResponse> result = service.getPortfolioPerformance(portfolioId, "1W");

        assertThat(result).hasSize(7)
            .allSatisfy(point -> assertThat(point.balance()).isEqualByComparingTo("125"));
        verifyNoInteractions(cache, marketService, instrumentService);
    }

    @Test
    void usesLookbackCloseForOpeningPositionsAcrossNonTradingDays() {
        Portfolio portfolio = portfolio();
        Instrument apple = instrument("AAPL", InstrumentType.EQUITY);
        addHolding(portfolio, apple, "2");
        when(cache.findOrInsertCandles("AAPL", InstrumentType.EQUITY, priceStart, end))
            .thenReturn(List.of(candle(apple, start.minusDays(3), "10"),
                candle(apple, start.plusDays(1), "12")));

        List<PerformanceResponse> result = service.getPortfolioPerformance(portfolioId, "1W");

        assertThat(result).hasSize(7);
        assertThat(result.getFirst().balance()).isEqualByComparingTo("20");
        assertThat(result.get(1).balance()).isEqualByComparingTo("24");
    }

    @Test
    void omitsDatesBeforeFirstHoldingAndExcludesTodayAndFutureEvents() {
        Portfolio portfolio = portfolio();
        Instrument usd = instrument("USD", InstrumentType.CASH);
        addHolding(portfolio, usd, "100", start.plusDays(2).atTime(23, 59).toInstant(ZoneOffset.UTC));
        addHolding(portfolio, usd, "10", start.plusDays(3).atStartOfDay(ZoneOffset.UTC).toInstant());
        addHolding(portfolio, instrument("AAPL", InstrumentType.EQUITY), "5",
            today.atStartOfDay(ZoneOffset.UTC).toInstant());
        addHolding(portfolio, usd, "999", today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());

        List<PerformanceResponse> result = service.getPortfolioPerformance(portfolioId, "1W");

        assertThat(result).hasSize(5);
        assertThat(result.getFirst().date()).isEqualTo(timestamp(start.plusDays(2)));
        assertThat(result.getFirst().balance()).isEqualByComparingTo("100");
        assertThat(result.get(1).balance()).isEqualByComparingTo("110");
        assertThat(result.getLast().date()).isEqualTo(timestamp(end));
        assertThat(result.getLast().balance()).isEqualByComparingTo("110");
        verifyNoInteractions(cache, marketService, instrumentService);
    }

    @Test
    void returnsEmptyWhenOnlyTodaysHoldingsExist() {
        Portfolio portfolio = portfolio();
        addHolding(portfolio, instrument("AAPL", InstrumentType.EQUITY), "1",
            today.atStartOfDay(ZoneOffset.UTC).toInstant());

        assertThat(service.getPortfolioPerformance(portfolioId, "1W")).isEmpty();
        verifyNoInteractions(cache, marketService, instrumentService);
    }

    @Test
    void missingPriceOnlySuppressesDatesWhenTheInstrumentIsHeld() {
        Portfolio portfolio = portfolio();
        Instrument apple = instrument("AAPL", InstrumentType.EQUITY);
        Instrument usd = instrument("USD", InstrumentType.CASH);
        addHolding(portfolio, usd, "100");
        addHolding(portfolio, apple, "1", start.plusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant());
        addHolding(portfolio, usd, "-10", start.plusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant());
        addHolding(portfolio, apple, "-1", start.plusDays(4).atStartOfDay(ZoneOffset.UTC).toInstant());
        addHolding(portfolio, usd, "15", start.plusDays(4).atStartOfDay(ZoneOffset.UTC).toInstant());
        when(cache.findOrInsertCandles("AAPL", InstrumentType.EQUITY, priceStart, end))
            .thenReturn(List.of());

        List<PerformanceResponse> result = service.getPortfolioPerformance(portfolioId, "1W");

        assertThat(result).extracting(PerformanceResponse::date).containsExactly(
            timestamp(start), timestamp(start.plusDays(1)), timestamp(start.plusDays(4)),
            timestamp(start.plusDays(5)), timestamp(end));
        assertThat(result.getLast().balance()).isEqualByComparingTo("105");
    }

    @Test
    void rejectsUndatedHoldingsRatherThanInventingHistory() {
        Portfolio portfolio = portfolio();
        addHolding(portfolio, instrument("USD", InstrumentType.CASH), "100", null);

        assertThatThrownBy(() -> service.getPortfolioPerformance(portfolioId, "1W"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("holding has no date");
        verifyNoInteractions(cache, marketService, instrumentService);
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
        addHolding(portfolio, instrument, quantity, today.minusYears(1).atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    private void addHolding(Portfolio portfolio, Instrument instrument, String quantity, Instant date) {
        portfolio.addHolding(new Holding(
            date, BigDecimal.ONE, new BigDecimal(quantity), instrument, null, portfolio
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