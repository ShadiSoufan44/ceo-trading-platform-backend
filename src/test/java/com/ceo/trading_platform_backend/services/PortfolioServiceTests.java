package com.ceo.trading_platform_backend.services;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.Portfolio;
import com.ceo.trading_platform_backend.repositories.PortfolioRepository;
import com.ceo.trading_platform_backend.dto.HoldingSummaryResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioService Tests")
public class PortfolioServiceTests {
    @Mock 
    private PortfolioRepository portfolioRepository;

    @Mock
    private InstrumentService instrumentService;

    @Mock
    private MarketService marketService;

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
            marketService
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
            List<MarketService.Candle> candles = List.of(
                new MarketService.Candle(today.minusDays(4), new BigDecimal("140"), new BigDecimal("142"), new BigDecimal("139"), new BigDecimal("140"), null, 100L, false),
                new MarketService.Candle(today, new BigDecimal("150"), new BigDecimal("152"), new BigDecimal("149"), new BigDecimal("150"), null, 100L, false)
            );

            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);
            when(marketService.getQuoteByType("AAPL", InstrumentType.EQUITY)).thenReturn(quote);
            when(marketService.getCandles("AAPL", today.minusYears(1), today)).thenReturn(candles);

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
            assertThat(summary.percentChange().get("5D")).isEqualByComparingTo("0.07142857");
            assertThat(summary.weekLow52()).isEqualByComparingTo("139");
            assertThat(summary.weekHigh52()).isEqualByComparingTo("152");
        }
    }

    @Nested
    @DisplayName("Test getTotalValueByPortfolioId()")
    class GetTotalValueByPortfolioIdTests {
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

            BigDecimal result = portfolioService.getTotalValueByPortfolioId(uuid1);

            assertThat(result).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return correct value when only cash holdings")
        void shouldReturnCorrectValueWithOnlyCashHoldings() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createCashHolding(new BigDecimal("5000"), portfolio));
            portfolio.addHolding(createCashHolding(new BigDecimal("3000"), portfolio));
            
            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);

            BigDecimal result = portfolioService.getTotalValueByPortfolioId(uuid1);

            assertThat(result).isEqualByComparingTo(new BigDecimal("8000"));
        }

        @Test
        @DisplayName("Should return correct value with cash and 1 non-cash holding")
        void shouldReturnCorrectValueWithCashAndOneNonCashHolding() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createCashHolding(new BigDecimal("5000"), portfolio));
            portfolio.addHolding(createStockHolding(new BigDecimal("10"), new BigDecimal("150"), portfolio));

            when(marketService.getPriceWithType(stockInstrument.getType(), stockInstrument.getSymbol()))
                .thenReturn(new BigDecimal("150"));
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

            when(marketService.getPriceWithType(stockInstrument.getType(), stockInstrument.getSymbol()))
                .thenReturn(new BigDecimal("150"))
                .thenReturn(new BigDecimal("200"));
            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);

            BigDecimal result = portfolioService.getTotalValueByPortfolioId(uuid1);

            // Total = 5000 + (1 * 10 * 150) + (1 * 5 * 200) = 7500
            assertThat(result).isEqualByComparingTo(new BigDecimal("7500"));
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