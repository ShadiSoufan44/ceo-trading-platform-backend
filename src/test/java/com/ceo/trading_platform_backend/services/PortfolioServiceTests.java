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

            assertThat(result).isEqualTo(new BigDecimal("5000"));
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

            assertThat(result).isEqualTo(new BigDecimal("1000"));
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

            assertThat(result).isEqualTo(new BigDecimal("8000"));
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
            assertThat(result).isEqualTo(new BigDecimal("6500"));
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
            assertThat(result).isEqualTo(new BigDecimal("7500"));
        }
    }
}