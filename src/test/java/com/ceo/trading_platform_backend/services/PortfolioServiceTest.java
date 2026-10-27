package com.ceo.trading_platform_backend.services;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.enums.PortfolioType;
import com.ceo.trading_platform_backend.enums.Side;
import com.ceo.trading_platform_backend.exception.ResourceNotFoundException;
import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.models.InstrumentCandle;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.Portfolio;
import com.ceo.trading_platform_backend.models.User;
import com.ceo.trading_platform_backend.enums.UserRole;
import com.ceo.trading_platform_backend.repositories.PortfolioRepository;
import com.ceo.trading_platform_backend.dto.HoldingSummaryResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioService Tests")
public class PortfolioServiceTest {
    @Mock 
    private PortfolioRepository portfolioRepository;

    @Mock
    private InstrumentService instrumentService;

    @Mock
    private MarketService marketService;

    @Mock
    private InstrumentCacheService instrumentCacheService;

    @Mock
    private UserService userService;

    private PortfolioService portfolioService;

    private Instrument usdInstrument;
    private Instrument stockInstrument;

    private final UUID uuid1 = UUID.randomUUID();
    private final UUID uuid2 = UUID.randomUUID();
    private final UUID uuid3 = UUID.randomUUID();
    private final UUID uuid4 = UUID.randomUUID();
    private final UUID uuid5 = UUID.randomUUID();


    @BeforeEach 
    void setup() {
        usdInstrument = new Instrument("USD", InstrumentType.CASH, "United States Dollar");
        stockInstrument = new Instrument("AAPL", InstrumentType.EQUITY, "Apple Inc.");
        
        portfolioService = new PortfolioService(
            portfolioRepository, 
            instrumentService,
            instrumentCacheService,
            marketService,
            userService
        );
        
        // Default mock behavior: USD has price of 1
        // when(marketService.getPrice("USD")).thenReturn(BigDecimal.ONE);
    }

    private Portfolio createTestPortfolio(UUID id, PortfolioType type) {
        Portfolio portfolio = new Portfolio(type);
        return portfolio;
    }

    private Order createTestOrder(UUID orderId) {
        Order order = new Order(
            null, 
            null, 
            null, 
            null, 
            null, 
            null, 
            null,
            null,
            null
        );
        return order;
    }

    @Test
    void createPortfolioPersistsSuppliedNameAndClient() {
        User client = new User("Test Client", "test@example.com", "hash", Instant.now(), UserRole.CLIENT);
        when(userService.getUserEntity(uuid1)).thenReturn(client);
        when(portfolioRepository.save(any(Portfolio.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Portfolio created = portfolioService.createPortfolio(
            PortfolioType.BROKERAGE, uuid1, "  Long-term Growth  ");

        assertThat(created.getName()).isEqualTo("Long-term Growth");
        assertThat(created.getClient()).isSameAs(client);
        verify(portfolioRepository).save(created);
    }

    @Test
    void createPortfolioRejectsBlankName() {
        assertThatThrownBy(() -> portfolioService.createPortfolio(
                PortfolioType.BROKERAGE, uuid1, "   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Portfolio name is required");
    }

    @Test
    void createPortfolioRejectsNameLongerThanMaximum() {
        assertThatThrownBy(() -> portfolioService.createPortfolio(
                PortfolioType.BROKERAGE, uuid1, "x".repeat(Portfolio.NAME_MAX_LENGTH + 1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Portfolio name must be at most 100 characters");
    }

    private Holding createCashHolding(BigDecimal quantity, Portfolio portfolio) {
        return new Holding(
            Instant.now(),
            BigDecimal.ONE,
            quantity,
            usdInstrument,
            createTestOrder(uuid1),
            portfolio
        );
    }

    private Holding createStockHolding(BigDecimal quantity, BigDecimal price, Portfolio portfolio) {
        return new Holding(
            Instant.now(),
            price,
            quantity,
            stockInstrument,
            createTestOrder(uuid2),
            portfolio
        );
    }

    @Nested
    @DisplayName("Test getPortfolioById()")
    class GetPortfolioByIdTests {
        @Test
        @DisplayName("Should return correct portfolio when it exists")
        void shouldReturnPortfolioWhenExists() {
            Portfolio expected = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(expected);

            Portfolio result = portfolioService.getPortfolioById(uuid1);

            assertThat(result).isNotNull().isEqualTo(expected);
            verify(portfolioRepository).getReferenceById(uuid1);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when portfolio ID doesn't exist")
        void shouldThrowExceptionWhenNotExists() {
            when(portfolioRepository.getReferenceById(uuid5)).thenThrow();

            assertThatThrownBy(() -> portfolioService.getPortfolioById(uuid5))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Portfolio not found with id");
            verify(portfolioRepository).getReferenceById(uuid5);
        }
    }

    @Nested
    @DisplayName("Test getBuyingPowerByPortfolioId()")
    class GetBuyingPowerByPortfolioIdTests {
        @Test
        @DisplayName("Should throw ResourceNotFoundException when portfolio ID doesn't exist")
        void shouldThrowExceptionWhenPortfolioNotExists() {
            when(portfolioRepository.getReferenceById(uuid5)).thenThrow();

            assertThatThrownBy(() -> portfolioService.getBuyingPowerByPortfolioId(uuid5))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Portfolio not found with id");
        }

        @Test
        @DisplayName("Should return 0 when portfolio has no cash holdings")
        void shouldReturnZeroWhenNoCashHoldings() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createStockHolding(new BigDecimal("10"), new BigDecimal("150"), portfolio));
            
            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);

            BigDecimal result = portfolioService.getBuyingPowerByPortfolioId(uuid1);

            assertThat(result).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return correct value with 1 cash holding")
        void shouldReturnCorrectValueWithOneCashHolding() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createCashHolding(new BigDecimal("5000"), portfolio));
            
            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);

            BigDecimal result = portfolioService.getBuyingPowerByPortfolioId(uuid1);

            assertThat(result).isEqualByComparingTo(new BigDecimal("5000"));
        }

        @Test
        @DisplayName("Should return correct value with multiple cash holdings")
        void shouldReturnCorrectValueWithMultipleCashHoldings() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createCashHolding(new BigDecimal("3000"), portfolio));
            portfolio.addHolding(createCashHolding(new BigDecimal("2000"), portfolio));
            portfolio.addHolding(createCashHolding(new BigDecimal("-4000"), portfolio));
            
            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);

            BigDecimal result = portfolioService.getBuyingPowerByPortfolioId(uuid1);

            assertThat(result).isEqualByComparingTo(new BigDecimal("1000"));
        }
    }

    @Nested
    @DisplayName("Test getHoldingsByPortfolioId()")
    class GetHoldingsByPortfolioIdTests {
        @Test
        @DisplayName("Should throw ResourceNotFoundException when portfolio ID doesn't exist")
        void shouldThrowExceptionWhenPortfolioNotExists() {
            when(portfolioRepository.getReferenceById(uuid5)).thenThrow();

            assertThatThrownBy(() -> portfolioService.getHoldingsByPortfolioId(uuid5))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Portfolio not found with id");
        }

        @Test
        @DisplayName("Should return empty list when portfolio has no holdings")
        void shouldReturnEmptyListWhenNoHoldings() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            
            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);

            List<Holding> result = portfolioService.getHoldingsByPortfolioId(uuid1);

            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should return list with 1 holding")
        void shouldReturnListWithOneHolding() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createCashHolding(new BigDecimal("1000"), portfolio));
            
            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);

            List<Holding> result = portfolioService.getHoldingsByPortfolioId(uuid1);

            assertThat(result)
                .isNotNull()
                .hasSize(1)
                .allMatch(h -> h != null);
        }

        @Test
        @DisplayName("Should return list with multiple holdings")
        void shouldReturnListWithMultipleHoldings() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createCashHolding(new BigDecimal("2000"), portfolio));
            portfolio.addHolding(createStockHolding(new BigDecimal("10"), new BigDecimal("150"), portfolio));
            portfolio.addHolding(createCashHolding(new BigDecimal("1000"), portfolio));
            
            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);

            List<Holding> result = portfolioService.getHoldingsByPortfolioId(uuid1);

            assertThat(result)
                .isNotNull()
                .hasSize(3)
                .allMatch(h -> h != null);
        }
    }

    @Nested
    @DisplayName("Test getHoldingSummariesByPortfolioId()")
    class GetHoldingSummariesByPortfolioIdTests {
        @Test
        @DisplayName("Should enrich database holding values with live market data")
        void shouldEnrichHoldingWithLiveMarketData() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createStockHolding(
                new BigDecimal("10"), new BigDecimal("125"), portfolio
            ));

            MarketService.QuoteData quote = new MarketService.QuoteData(
                "AAPL", new BigDecimal("151.50"), new BigDecimal("151.40"),
                new BigDecimal("151.60"), new BigDecimal("13.20"), "USD",
                new BigDecimal("1.50"), new BigDecimal("0.01"),
                new BigDecimal("150.00"), Instant.now(), "OPEN"
            );
            LocalDate today = LocalDate.now();
            List<InstrumentCandle> candles = List.of(
                new InstrumentCandle(
                    stockInstrument,
                    today.minusDays(4).atStartOfDay(ZoneOffset.UTC).toInstant(),
                    new BigDecimal("140"), new BigDecimal("142"), new BigDecimal("139"),
                    new BigDecimal("140"), null, new BigDecimal("100"), false
                ),
                new InstrumentCandle(
                    stockInstrument,
                    today.atStartOfDay(ZoneOffset.UTC).toInstant(),
                    new BigDecimal("150"), new BigDecimal("152"), new BigDecimal("149"),
                    new BigDecimal("150"), null, new BigDecimal("100"), false
                )
            );

            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);
            when(marketService.normalizeSymbolByType("AAPL", InstrumentType.EQUITY)).thenReturn("AAPL");
            when(marketService.getQuotes(List.of("AAPL"))).thenReturn(List.of(
                new MarketService.BatchQuoteResult("AAPL", "test", false, quote, null)
            ));
            when(instrumentCacheService.findOrInsertCandles(
                "AAPL", InstrumentType.EQUITY, today.minusYears(1).minusDays(7), today
            )).thenReturn(candles);

            List<HoldingSummaryResponse> result = portfolioService.getHoldingSummariesByPortfolioId(uuid1);

            assertThat(result).hasSize(1);
            HoldingSummaryResponse summary = result.get(0);
            assertThat(summary.portfolioId()).isEqualTo(uuid1);
            assertThat(summary.symbol()).isEqualTo("AAPL");
            assertThat(summary.quantity()).isEqualByComparingTo("10");
            assertThat(summary.avgCost()).isEqualByComparingTo("125");
            assertThat(summary.currentPrice()).isEqualByComparingTo("151.50");
            assertThat(summary.dayChangeValue()).isEqualByComparingTo("1.50");
            assertThat(summary.percentChange().get("1D")).isEqualByComparingTo("0.01");
            assertThat(summary.weekLow52()).isEqualByComparingTo("139");
            assertThat(summary.weekHigh52()).isEqualByComparingTo("152");
        }

        @Test
        @DisplayName("Should include USD cash holdings without requesting a market quote")
        void shouldSummarizeUsdCashHoldingWithoutMarketQuote() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createCashHolding(new BigDecimal("5000"), portfolio));
            LocalDate today = LocalDate.now();

            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);
            when(instrumentCacheService.findOrInsertCandles(
                "USD", InstrumentType.CASH, today.minusYears(1).minusDays(7), today
            )).thenReturn(List.of());

            List<HoldingSummaryResponse> result = portfolioService.getHoldingSummariesByPortfolioId(uuid1);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).symbol()).isEqualTo("USD");
            assertThat(result.get(0).currentPrice()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(result.get(0).weekLow52()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(result.get(0).weekHigh52()).isEqualByComparingTo(BigDecimal.ONE);
            verifyNoInteractions(marketService);
        }

        // AAPL on Thu 2026-10-22 (real API data): live price 348.22, previous close 343.99,
        // and the API's changePercent is in percent (1.2297 = 1.23%)
        private final Instant asOf = Instant.parse("2026-10-22T20:00:01Z");

        private MarketService.QuoteData aaplQuote() {
            return new MarketService.QuoteData(
                "AAPL", new BigDecimal("348.22"), new BigDecimal("348.18"),
                new BigDecimal("348.26"), new BigDecimal("1.8467"), "USD",
                new BigDecimal("4.23"), new BigDecimal("1.2296869095031833"),
                new BigDecimal("343.99"), asOf, "unknown"
            );
        }

        private InstrumentCandle candle(String date, String close) {
            BigDecimal price = new BigDecimal(close);
            return new InstrumentCandle(
                stockInstrument, LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant(),
                price, price, price, price, null, new BigDecimal("100"), false
            );
        }

        private HoldingSummaryResponse summaryFor(List<InstrumentCandle> candles) {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createStockHolding(new BigDecimal("10"), new BigDecimal("125"), portfolio));
            LocalDate today = LocalDate.now();

            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);
            when(marketService.normalizeSymbolByType("AAPL", InstrumentType.EQUITY)).thenReturn("AAPL");
            when(marketService.getQuotes(List.of("AAPL"))).thenReturn(List.of(
                new MarketService.BatchQuoteResult("AAPL", "test", false, aaplQuote(), null)
            ));
            when(instrumentCacheService.findOrInsertCandles(
                "AAPL", InstrumentType.EQUITY, today.minusYears(1).minusDays(7), today
            )).thenReturn(candles);

            return portfolioService.getHoldingSummariesByPortfolioId(uuid1).get(0);
        }

        // real daily closes, including today's partial candle which must be ignored
        private final List<InstrumentCandle> recentCandles = List.of(
            candle("2026-10-12", "340.97"),
            candle("2026-10-13", "341.27"),
            candle("2026-10-14", "342.96"),
            candle("2026-10-15", "342.00"),
            candle("2026-10-16", "343.46"),
            candle("2026-10-19", "342.94"),
            candle("2026-10-20", "339.50"),
            candle("2026-10-21", "343.99"),
            candle("2026-10-22", "343.03")
        );

        @Test
        @DisplayName("1D is a fraction worked out from the previous close, not the API's percent value")
        void shouldReturnOneDayChangeAsFraction() {
            HoldingSummaryResponse summary = summaryFor(recentCandles);

            // (348.22 - 343.99) / 343.99
            assertThat(summary.percentChange().get("1D")).isEqualByComparingTo("0.01229687");
        }

        @Test
        @DisplayName("5D starts from the close five trading sessions back and ends at the live price")
        void shouldMeasureFiveDaysInTradingSessions() {
            HoldingSummaryResponse summary = summaryFor(recentCandles);

            // Thu 10-22 vs the close on Thu 10-15 (342.00), skipping the weekend and today's partial candle
            assertThat(summary.percentChange().get("5D")).isEqualByComparingTo("0.01818713");
        }

        @Test
        @DisplayName("Month periods start from the last close on or before that calendar date")
        void shouldStartMonthPeriodsFromLastCloseOnOrBeforeDate() {
            List<InstrumentCandle> candles = new java.util.ArrayList<>(List.of(
                candle("2025-10-17", "200.00"),   // Fri, last close on or before Wed 2025-10-22 -> 1Y start
                candle("2025-10-23", "999.00"),
                candle("2026-04-22", "300.00"),   // 6M start, on the date itself
                candle("2026-09-21", "330.00"),   // Mon, last close on or before Tue 2026-09-22 -> 1M start
                candle("2026-09-23", "999.00")
            ));
            candles.addAll(recentCandles);

            HoldingSummaryResponse summary = summaryFor(candles);

            assertThat(summary.percentChange().get("1M")).isEqualByComparingTo("0.05521212");   // 348.22 / 330 - 1
            assertThat(summary.percentChange().get("6M")).isEqualByComparingTo("0.16073333");   // 348.22 / 300 - 1
            assertThat(summary.percentChange().get("1Y")).isEqualByComparingTo("0.74110000");   // 348.22 / 200 - 1
        }

        @Test
        @DisplayName("Periods without enough history are null instead of a made-up number")
        void shouldReturnNullWhenHistoryIsTooShort() {
            HoldingSummaryResponse summary = summaryFor(recentCandles);

            assertThat(summary.percentChange().get("1M")).isNull();
            assertThat(summary.percentChange().get("6M")).isNull();
            assertThat(summary.percentChange().get("1Y")).isNull();
        }

        @Test
        @DisplayName("52-week range ignores the extra lookback candles older than a year")
        void shouldLimitFiftyTwoWeekRangeToOneYear() {
            List<InstrumentCandle> candles = new java.util.ArrayList<>(recentCandles);
            candles.add(candle("2025-10-17", "100.00"));

            HoldingSummaryResponse summary = summaryFor(candles);

            assertThat(summary.weekLow52()).isEqualByComparingTo("339.50");
        }
    }

    @Nested
    @DisplayName("Test getTotalValueByPortfolioId()")
    class GetTotalValueByPortfolioIdTests {
        private void mockHoldingQuotes(Portfolio portfolio, String... prices) {
            assertThat(prices).hasSize(portfolio.getHoldings().size());
            List<String> symbols = new ArrayList<>();
            List<MarketService.BatchQuoteResult> quotes = new ArrayList<>();
            for (int i = 0; i < prices.length; i++) {
                Instrument instrument = portfolio.getHoldings().get(i).getInstrument();
                if (instrument.getType() == InstrumentType.CASH && instrument.getSymbol().equals("USD")) {
                    continue;
                }
                String symbol = instrument.getSymbol();
                if (symbols.contains(symbol)) {
                    continue;
                }
                when(marketService.normalizeSymbolByType(symbol, instrument.getType()))
                    .thenReturn(symbol);
                symbols.add(symbol);
                MarketService.QuoteData quote = new MarketService.QuoteData(
                    symbol, new BigDecimal(prices[i]), null, null, null, "USD",
                    null, null, null, Instant.now(), "OPEN"
                );
                quotes.add(new MarketService.BatchQuoteResult(symbol, "test", false, quote, null));
            }
            if (!symbols.isEmpty()) {
                when(marketService.getQuotes(symbols)).thenReturn(quotes);
            }
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when portfolio ID doesn't exist")
        void shouldThrowExceptionWhenPortfolioNotExists() {
            when(portfolioRepository.getReferenceById(uuid5)).thenThrow(new ResourceNotFoundException("Portfolio not found with id " + uuid5));

            assertThatThrownBy(() -> portfolioService.getTotalValueByPortfolioId(uuid5))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Portfolio not found with id");
        }

        @Test
        @DisplayName("Should return 0 when portfolio has no holdings")
        void shouldReturnZeroWhenNoHoldings() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            
            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);
            mockHoldingQuotes(portfolio);

            BigDecimal result = portfolioService.getTotalValueByPortfolioId(uuid1);

            assertThat(result).isEqualTo(BigDecimal.ZERO);
            verify(portfolioRepository).getReferenceById(uuid1);
            verifyNoMoreInteractions(portfolioRepository);
            verifyNoMoreInteractions(marketService);
        }

        @Test
        @DisplayName("Should return correct value when only cash holdings")
        void shouldReturnCorrectValueWithOnlyCashHoldings() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createCashHolding(new BigDecimal("5000"), portfolio));
            portfolio.addHolding(createCashHolding(new BigDecimal("3000"), portfolio));
            
            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);
            mockHoldingQuotes(portfolio, "1", "1");

            BigDecimal result = portfolioService.getTotalValueByPortfolioId(uuid1);

            assertThat(result).isEqualByComparingTo(new BigDecimal("8000"));
            verifyNoInteractions(marketService);
        }

        @Test
        @DisplayName("Should return correct value with cash and 1 non-cash holding")
        void shouldReturnCorrectValueWithCashAndOneNonCashHolding() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createCashHolding(new BigDecimal("5000"), portfolio));
            portfolio.addHolding(createStockHolding(new BigDecimal("10"), new BigDecimal("150"), portfolio));

            mockHoldingQuotes(portfolio, "1", "150");
            when(portfolioRepository.getReferenceById(uuid1))
                .thenReturn(portfolio);

            BigDecimal result = portfolioService.getTotalValueByPortfolioId(uuid1);

            // Total = 5000 + (1 * 10 * 150) = 6500
            assertThat(result).isEqualByComparingTo(new BigDecimal("6500"));
        }

        @Test
        @DisplayName("Should return correct value with cash and 2+ non-cash holdings")
        void shouldReturnCorrectValueWithCashAndMultipleNonCashHoldings() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createCashHolding(new BigDecimal("5000"), portfolio));
            portfolio.addHolding(createStockHolding(new BigDecimal("10"), new BigDecimal("150"), portfolio));
            portfolio.addHolding(createStockHolding(new BigDecimal("5"), new BigDecimal("200"), portfolio));

            mockHoldingQuotes(portfolio, "1", "150", "150");
            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);

            BigDecimal result = portfolioService.getTotalValueByPortfolioId(uuid1);

            // Both holdings refer to the same instrument, so they share its current quote.
            assertThat(result).isEqualByComparingTo(new BigDecimal("7250"));
        }

        @Test
        @DisplayName("Should value fractional holdings at current market prices")
        void shouldValueFractionalHoldingsAtCurrentMarketPrices() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.addHolding(createCashHolding(new BigDecimal("100.25"), portfolio));
            portfolio.addHolding(createStockHolding(new BigDecimal("2.5"), new BigDecimal("100"), portfolio));

            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);
            mockHoldingQuotes(portfolio, "1", "150.50");

            BigDecimal result = portfolioService.getTotalValueByPortfolioId(uuid1);

            assertThat(result).isEqualByComparingTo("476.50");
            verify(portfolioRepository).getReferenceById(uuid1);
            verifyNoMoreInteractions(portfolioRepository);
        }

        @Test
        @DisplayName("Should subtract negative cash and stock holdings")
        void shouldSubtractNegativeHoldings() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.addHolding(createCashHolding(new BigDecimal("5000"), portfolio));
            portfolio.addHolding(createCashHolding(new BigDecimal("-1000"), portfolio));
            portfolio.addHolding(createStockHolding(new BigDecimal("10"), new BigDecimal("100"), portfolio));
            portfolio.addHolding(createStockHolding(new BigDecimal("-4"), new BigDecimal("125"), portfolio));

            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);
            mockHoldingQuotes(portfolio, "1", "1", "150", "150");

            BigDecimal result = portfolioService.getTotalValueByPortfolioId(uuid1);

            assertThat(result).isEqualByComparingTo("4900");
        }
    }

    @Nested
    @DisplayName("Test updateHoldingsFromOrder()")
    class UpdateHoldingsFromOrderTests {
        private Portfolio portfolio;
        private Order buyOrder;
        private Order sellOrder;
        private final UUID USDInstrumentUuid = UUID.randomUUID();
        private final UUID AAPLInstrumentUuid = UUID.randomUUID();
        private final UUID portfolioId = UUID.randomUUID();
        private final UUID clientId = UUID.randomUUID();
        private final UUID unusedId = UUID.randomUUID();

        @BeforeEach
        void setupOrders() {
            portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            
            buyOrder = new Order(
                AAPLInstrumentUuid,  // instrumentId (AAPL)
                USDInstrumentUuid, // instrumentId (USD)
                Side.BUY,
                portfolioId,  // portfolioId
                clientId,  // clientId
                new BigDecimal("150"),  // quote
                null,
                new BigDecimal("10"),  // quantity
                "America/New_York"
            );
            
            sellOrder = new Order(
                USDInstrumentUuid, // instrumentId (USD)
                AAPLInstrumentUuid,  // instrumentId (AAPL)
                Side.SELL,
                portfolioId,  // portfolioId
                clientId,  // clientId
                new BigDecimal("200"),  // quote
                null,
                new BigDecimal("5"),  // quantity
                "America/New_York"
            );
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when portfolio doesn't exist")
        void shouldThrowExceptionWhenPortfolioNotExists() {
            when(portfolioRepository.getReferenceById(unusedId))
                .thenThrow(new ResourceNotFoundException("Portfolio not found with id " + uuid5));

            assertThatThrownBy(() -> portfolioService.updateHoldingsFromOrder(buyOrder, new BigDecimal("150"), BigDecimal.ONE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Portfolio not found with id");
        }

        @Test
        @DisplayName("BUY order: should create holdings with correct cash and instrument quantities")
        void shouldCreateHoldingsForBuyOrderWithCorrectQuantities() {
            when(portfolioRepository.getReferenceById(portfolioId)).thenReturn(portfolio);
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(instrumentService.getInstrumentById(AAPLInstrumentUuid))
                .thenReturn(stockInstrument);
            when(instrumentService.getInstrumentById(USDInstrumentUuid))
                .thenReturn(usdInstrument);

            portfolioService.updateHoldingsFromOrder(buyOrder, new BigDecimal("150"), BigDecimal.ONE);

            // Verify holdings were added: cash holding (negative) and instrument holding (positive)
            assertThat(portfolio.getHoldings()).hasSize(2);
            
            // Find cash and instrument holdings
            Holding cashHolding = portfolio.getHoldings().stream()
                .filter(h -> h.getInstrument().getSymbol().equals("USD"))
                .findFirst()
                .orElse(null);
            Holding instrumentHolding = portfolio.getHoldings().stream()
                .filter(h -> h.getInstrument().getSymbol().equals("AAPL"))
                .findFirst()
                .orElse(null);

            assertThat(cashHolding).isNotNull();
            assertThat(instrumentHolding).isNotNull();
            
            // BUY: cash should be negative (deducted), instrument should be positive
            assertThat(cashHolding.getQuantity())
                .isEqualByComparingTo(new BigDecimal("-1500"));
            assertThat(instrumentHolding.getQuantity())
                .isEqualByComparingTo(new BigDecimal("10"));
        }

        @Test
        @DisplayName("BUY order: cash holding should have price 1 and correct instrument")
        void shouldCreateBuyOrderCashHoldingWithCorrectProperties() {
            when(portfolioRepository.getReferenceById(portfolioId)).thenReturn(portfolio);
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(instrumentService.getInstrumentById(AAPLInstrumentUuid))
                .thenReturn(stockInstrument);
            when(instrumentService.getInstrumentById(USDInstrumentUuid))
                .thenReturn(usdInstrument);

            portfolioService.updateHoldingsFromOrder(buyOrder, new BigDecimal("150"), BigDecimal.ONE);

            Holding cashHolding = portfolio.getHoldings().stream()
                .filter(h -> h.getInstrument().getSymbol().equals("USD"))
                .findFirst()
                .orElse(null);

            assertThat(cashHolding).isNotNull();
            assertThat(cashHolding.getPurchasedPrice()).isEqualTo(BigDecimal.ONE);
            assertThat(cashHolding.getInstrument().getSymbol()).isEqualTo("USD");
            assertThat(cashHolding.getQuantity())
                .isEqualByComparingTo(new BigDecimal("-1500"));
        }

        @Test
        @DisplayName("BUY order: instrument holding should have correct price and quantity")
        void shouldCreateBuyOrderInstrumentHoldingWithCorrectProperties() {
            when(portfolioRepository.getReferenceById(portfolioId)).thenReturn(portfolio);
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(instrumentService.getInstrumentById(AAPLInstrumentUuid))
                .thenReturn(stockInstrument);
            when(instrumentService.getInstrumentById(USDInstrumentUuid))
                .thenReturn(usdInstrument);

            portfolioService.updateHoldingsFromOrder(buyOrder, new BigDecimal("150"), BigDecimal.ONE);

            Holding instrumentHolding = portfolio.getHoldings().stream()
                .filter(h -> h.getInstrument().getSymbol().equals("AAPL"))
                .findFirst()
                .orElse(null);

            assertThat(instrumentHolding).isNotNull();
            assertThat(instrumentHolding.getPurchasedPrice())
                .isEqualByComparingTo(new BigDecimal("150"));
            assertThat(instrumentHolding.getInstrument().getSymbol())
                .isEqualTo("AAPL");
            assertThat(instrumentHolding.getQuantity())
                .isEqualByComparingTo(new BigDecimal("10"));
        }

        @Test
        @DisplayName("SELL order: should create holdings with correct cash and instrument quantities")
        void shouldCreateHoldingsForSellOrderWithCorrectQuantities() {
            when(portfolioRepository.getReferenceById(portfolioId)).thenReturn(portfolio);
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(instrumentService.getInstrumentById(AAPLInstrumentUuid))
                .thenReturn(stockInstrument);
            when(instrumentService.getInstrumentById(USDInstrumentUuid))
                .thenReturn(usdInstrument);

            portfolioService.updateHoldingsFromOrder(sellOrder, BigDecimal.ONE, new BigDecimal("200"));

            // Verify holdings were added: cash holding (positive) and instrument holding (negative)
            assertThat(portfolio.getHoldings()).hasSize(2);
            
            Holding cashHolding = portfolio.getHoldings().stream()
                .filter(h -> h.getInstrument().getSymbol().equals("USD"))
                .findFirst()
                .orElse(null);
            Holding instrumentHolding = portfolio.getHoldings().stream()
                .filter(h -> h.getInstrument().getSymbol().equals("AAPL"))
                .findFirst()
                .orElse(null);

            assertThat(cashHolding).isNotNull();
            assertThat(instrumentHolding).isNotNull();
            
            // SELL: cash should be positive (added), instrument should be negative
            assertThat(cashHolding.getQuantity())
                .isEqualByComparingTo(new BigDecimal("1000"));
            assertThat(instrumentHolding.getQuantity())
                .isEqualByComparingTo(new BigDecimal("-5"));
        }

        @Test
        @DisplayName("SELL order: cash holding should have price 1 and correct instrument")
        void shouldCreateSellOrderCashHoldingWithCorrectProperties() {
            when(portfolioRepository.getReferenceById(portfolioId)).thenReturn(portfolio);
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(instrumentService.getInstrumentById(AAPLInstrumentUuid))
                .thenReturn(stockInstrument);
            when(instrumentService.getInstrumentById(USDInstrumentUuid))
                .thenReturn(usdInstrument);

            portfolioService.updateHoldingsFromOrder(sellOrder, BigDecimal.ONE, new BigDecimal("200"));

            Holding cashHolding = portfolio.getHoldings().stream()
                .filter(h -> h.getInstrument().getSymbol().equals("USD"))
                .findFirst()
                .orElse(null);

            assertThat(cashHolding).isNotNull();
            assertThat(cashHolding.getPurchasedPrice()).isEqualTo(BigDecimal.ONE);
            assertThat(cashHolding.getInstrument().getSymbol()).isEqualTo("USD");
            assertThat(cashHolding.getQuantity())
                .isEqualByComparingTo(new BigDecimal("1000"));
        }

        @Test
        @DisplayName("SELL order: instrument holding should have correct price and negative quantity")
        void shouldCreateSellOrderInstrumentHoldingWithCorrectProperties() {
            when(portfolioRepository.getReferenceById(portfolioId)).thenReturn(portfolio);
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(instrumentService.getInstrumentById(AAPLInstrumentUuid))
                .thenReturn(stockInstrument);
            when(instrumentService.getInstrumentById(USDInstrumentUuid))
                .thenReturn(usdInstrument);

            portfolioService.updateHoldingsFromOrder(sellOrder, BigDecimal.ONE, new BigDecimal("200"));

            Holding instrumentHolding = portfolio.getHoldings().stream()
                .filter(h -> h.getInstrument().getSymbol().equals("AAPL"))
                .findFirst()
                .orElse(null);

            assertThat(instrumentHolding).isNotNull();
            assertThat(instrumentHolding.getPurchasedPrice())
                .isEqualByComparingTo(new BigDecimal("200"));
            assertThat(instrumentHolding.getInstrument().getSymbol()).isEqualTo("AAPL");
            assertThat(instrumentHolding.getQuantity())
                .isEqualByComparingTo(new BigDecimal("-5"));
        }

        @Test
        @DisplayName("BUY order with fractional shares: should handle BigDecimal quantities correctly")
        void shouldHandleFractionalSharesForBuyOrder() {
            Order fractionalOrder = new Order(
                AAPLInstrumentUuid,
                USDInstrumentUuid,  
                Side.BUY,
                portfolioId,  // portfolioId
                clientId,  // clientId
                new BigDecimal("125.0"),  // quote
                null,
                new BigDecimal("2.5"),  // fractional quantity
                "America/New_York"
            );

            when(portfolioRepository.getReferenceById(portfolioId))
                .thenReturn(portfolio);
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(instrumentService.getInstrumentById(AAPLInstrumentUuid))
                .thenReturn(stockInstrument);
            when(instrumentService.getInstrumentById(USDInstrumentUuid))
                .thenReturn(usdInstrument);

            portfolioService.updateHoldingsFromOrder(fractionalOrder, new BigDecimal("125.0"), BigDecimal.ONE);

            Holding cashHolding = portfolio.getHoldings().stream()
                .filter(h -> h.getInstrument().getSymbol().equals("USD"))
                .findFirst()
                .orElse(null);

            // Cash = -125.0 * 2.5 = -376.25
            assertThat(cashHolding.getQuantity())
                .isEqualByComparingTo((new BigDecimal("-312.5")));
        }

        @Test
        @DisplayName("SELL order with fractional shares: should handle BigDecimal quantities correctly")
        void shouldHandleFractionalSharesForSellOrder() {
            Order fractionalOrder = new Order(
                USDInstrumentUuid,
                AAPLInstrumentUuid,
                Side.SELL,
                portfolioId,  // portfolioId
                clientId,  // clientId
                new BigDecimal("200.75"),  // quote
                null,
                new BigDecimal("3.5"),  // fractional quantity
                "America/New_York"
            );

            when(portfolioRepository.getReferenceById(portfolioId)).thenReturn(portfolio);
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(instrumentService.getInstrumentById(AAPLInstrumentUuid))
                .thenReturn(stockInstrument);
            when(instrumentService.getInstrumentById(USDInstrumentUuid))
                .thenReturn(usdInstrument);

            portfolioService.updateHoldingsFromOrder(fractionalOrder, BigDecimal.ONE, new BigDecimal("200.75"));

            Holding cashHolding = portfolio.getHoldings().stream()
                .filter(h -> h.getInstrument().getSymbol().equals("USD"))
                .findFirst()
                .orElse(null);

            // Cash = 200.75 * 3.5 = 702.625
            assertThat(cashHolding.getQuantity())
                .isEqualByComparingTo(new BigDecimal("702.625"));
        }
    }
}