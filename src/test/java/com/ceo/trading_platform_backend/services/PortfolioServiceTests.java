package com.ceo.trading_platform_backend.services;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

            when(marketService.getPrice(stockInstrument.getSymbol()))
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

            when(marketService.getPrice(stockInstrument.getSymbol()))
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

        @BeforeEach
        void setupOrders() {
            portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            
            buyOrder = new Order(
                uuid2,  // instrumentId (AAPL)
                Side.BUY,
                uuid1,  // portfolioId
                uuid3,  // clientId
                new BigDecimal("150"),  // quote
                null,
                new BigDecimal("10")  // quantity
            );
            
            sellOrder = new Order(
                uuid2,  // instrumentId (AAPL)
                Side.SELL,
                uuid1,  // portfolioId
                uuid3,  // clientId
                new BigDecimal("200"),  // quote
                null,
                new BigDecimal("5")  // quantity
            );
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when portfolio doesn't exist")
        void shouldThrowExceptionWhenPortfolioNotExists() {
            when(portfolioRepository.getReferenceById(uuid5))
                .thenThrow(new ResourceNotFoundException("Portfolio not found with id " + uuid5));

            assertThatThrownBy(() -> portfolioService.updateHoldingsFromOrder(buyOrder, new BigDecimal("150")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Portfolio not found with id");
        }

        @Test
        @DisplayName("BUY order: should create holdings with correct cash and instrument quantities")
        void shouldCreateHoldingsForBuyOrderWithCorrectQuantities() {
            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(instrumentService.getInstrumentById(uuid2)).thenReturn(stockInstrument);
            when(instrumentService.getInstrumentBySymbol("USD")).thenReturn(usdInstrument);

            portfolioService.updateHoldingsFromOrder(buyOrder, new BigDecimal("150"));

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
            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(instrumentService.getInstrumentById(uuid2)).thenReturn(stockInstrument);
            when(instrumentService.getInstrumentBySymbol("USD")).thenReturn(usdInstrument);

            portfolioService.updateHoldingsFromOrder(buyOrder, new BigDecimal("150"));

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
            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(instrumentService.getInstrumentById(uuid2)).thenReturn(stockInstrument);
            when(instrumentService.getInstrumentBySymbol("USD")).thenReturn(usdInstrument);

            portfolioService.updateHoldingsFromOrder(buyOrder, new BigDecimal("150"));

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
            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(instrumentService.getInstrumentById(uuid2)).thenReturn(stockInstrument);
            when(instrumentService.getInstrumentBySymbol("USD")).thenReturn(usdInstrument);

            portfolioService.updateHoldingsFromOrder(sellOrder, new BigDecimal("200"));

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
            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(instrumentService.getInstrumentById(uuid2)).thenReturn(stockInstrument);
            when(instrumentService.getInstrumentBySymbol("USD")).thenReturn(usdInstrument);

            portfolioService.updateHoldingsFromOrder(sellOrder, new BigDecimal("200"));

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
            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(instrumentService.getInstrumentById(uuid2)).thenReturn(stockInstrument);
            when(instrumentService.getInstrumentBySymbol("USD")).thenReturn(usdInstrument);

            portfolioService.updateHoldingsFromOrder(sellOrder, new BigDecimal("200"));

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
        @DisplayName("Should call repository.save() twice")
        void shouldCallRepositorySaveTwice() {
            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(instrumentService.getInstrumentById(uuid2)).thenReturn(stockInstrument);
            when(instrumentService.getInstrumentBySymbol("USD")).thenReturn(usdInstrument);

            portfolioService.updateHoldingsFromOrder(buyOrder, new BigDecimal("150"));

            verify(portfolioRepository).getReferenceById(uuid1);
            verify(portfolioRepository, org.mockito.Mockito.times(2)).save(portfolio);
        }

        @Test
        @DisplayName("BUY order with fractional shares: should handle BigDecimal quantities correctly")
        void shouldHandleFractionalSharesForBuyOrder() {
            Order fractionalOrder = new Order(
                uuid2,  // instrumentId
                Side.BUY,
                uuid1,  // portfolioId
                uuid3,  // clientId
                new BigDecimal("125.0"),  // quote
                null,
                new BigDecimal("2.5")  // fractional quantity
            );

            when(portfolioRepository.getReferenceById(uuid1))
                .thenReturn(portfolio);
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(instrumentService.getInstrumentById(uuid2)).thenReturn(stockInstrument);
            when(instrumentService.getInstrumentBySymbol("USD")).thenReturn(usdInstrument);

            portfolioService.updateHoldingsFromOrder(fractionalOrder, new BigDecimal("125.0"));

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
                uuid2,  // instrumentId
                Side.SELL,
                uuid1,  // portfolioId
                uuid3,  // clientId
                new BigDecimal("200.75"),  // quote
                null,
                new BigDecimal("3.5")  // fractional quantity
            );

            when(portfolioRepository.getReferenceById(uuid1)).thenReturn(portfolio);
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(instrumentService.getInstrumentById(uuid2)).thenReturn(stockInstrument);
            when(instrumentService.getInstrumentBySymbol("USD")).thenReturn(usdInstrument);

            portfolioService.updateHoldingsFromOrder(fractionalOrder, new BigDecimal("200.75"));

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