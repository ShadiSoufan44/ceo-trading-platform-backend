package com.ceo.trading_platform_backend.services;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    @InjectMocks 
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

    private Holding createCashHolding(BigDecimal quantity) {
        return new Holding(
            Instant.now(),
            BigDecimal.ONE,
            quantity,
            usdInstrument,
            createTestOrder(uuid1)
        );
    }

    private Holding createStockHolding(BigDecimal quantity, BigDecimal price) {
        return new Holding(
            Instant.now(),
            price,
            quantity,
            stockInstrument,
            createTestOrder(uuid2)
        );
    }

    @Nested
    @DisplayName("Test getPortfolioById()")
    class GetPortfolioByIdTests {
        @Test
        @DisplayName("Should return correct portfolio when it exists")
        void shouldReturnPortfolioWhenExists() {
            Portfolio expected = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            when(portfolioRepository.findById(uuid1)).thenReturn(Optional.of(expected));

            Portfolio result = portfolioService.getPortfolioById(uuid1);

            assertThat(result).isNotNull().isEqualTo(expected);
            verify(portfolioRepository).findById(uuid1);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when portfolio ID doesn't exist")
        void shouldThrowExceptionWhenNotExists() {
            when(portfolioRepository.findById(uuid5)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> portfolioService.getPortfolioById(uuid5))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Portfolio not found with id");
            verify(portfolioRepository).findById(uuid5);
        }
    }

    @Nested
    @DisplayName("Test getBuyingPowerByPortfolioId()")
    class GetBuyingPowerByPortfolioIdTests {
        @Test
        @DisplayName("Should throw ResourceNotFoundException when portfolio ID doesn't exist")
        void shouldThrowExceptionWhenPortfolioNotExists() {
            when(portfolioRepository.findById(uuid5)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> portfolioService.getBuyingPowerByPortfolioId(uuid5))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Portfolio not found with id");
        }

        @Test
        @DisplayName("Should return 0 when portfolio has no cash holdings")
        void shouldReturnZeroWhenNoCashHoldings() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createStockHolding(new BigDecimal("10"), new BigDecimal("150")));
            
            when(portfolioRepository.findById(uuid1)).thenReturn(Optional.of(portfolio));

            BigDecimal result = portfolioService.getBuyingPowerByPortfolioId(uuid1);

            assertThat(result).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return correct value with 1 cash holding")
        void shouldReturnCorrectValueWithOneCashHolding() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createCashHolding(new BigDecimal("5000")));
            
            when(portfolioRepository.findById(uuid1)).thenReturn(Optional.of(portfolio));

            BigDecimal result = portfolioService.getBuyingPowerByPortfolioId(uuid1);

            assertThat(result).isEqualTo(new BigDecimal("5000"));
        }

        @Test
        @DisplayName("Should return correct value with multiple cash holdings")
        void shouldReturnCorrectValueWithMultipleCashHoldings() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createCashHolding(new BigDecimal("3000")));
            portfolio.addHolding(createCashHolding(new BigDecimal("2000")));
            
            when(portfolioRepository.findById(uuid1)).thenReturn(Optional.of(portfolio));

            BigDecimal result = portfolioService.getBuyingPowerByPortfolioId(uuid1);

            assertThat(result).isEqualTo(new BigDecimal("5000"));
        }
    }

    @Nested
    @DisplayName("Test getHoldingsByPortfolioId()")
    class GetHoldingsByPortfolioIdTests {
        @Test
        @DisplayName("Should throw ResourceNotFoundException when portfolio ID doesn't exist")
        void shouldThrowExceptionWhenPortfolioNotExists() {
            when(portfolioRepository.findById(uuid5)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> portfolioService.getHoldingsByPortfolioId(uuid5))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Portfolio not found with id");
        }

        @Test
        @DisplayName("Should return empty list when portfolio has no holdings")
        void shouldReturnEmptyListWhenNoHoldings() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            
            when(portfolioRepository.findById(uuid1)).thenReturn(Optional.of(portfolio));

            List<Holding> result = portfolioService.getHoldingsByPortfolioId(uuid1);

            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should return list with 1 holding")
        void shouldReturnListWithOneHolding() {
            Portfolio portfolio = createTestPortfolio(uuid1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createCashHolding(new BigDecimal("1000")));
            
            when(portfolioRepository.findById(uuid1)).thenReturn(Optional.of(portfolio));

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
            portfolio.addHolding(createCashHolding(new BigDecimal("2000")));
            portfolio.addHolding(createStockHolding(new BigDecimal("10"), new BigDecimal("150")));
            portfolio.addHolding(createCashHolding(new BigDecimal("1000")));
            
            when(portfolioRepository.findById(uuid1)).thenReturn(Optional.of(portfolio));

            List<Holding> result = portfolioService.getHoldingsByPortfolioId(uuid1);

            assertThat(result)
                .isNotNull()
                .hasSize(3)
                .allMatch(h -> h != null);
        }
    }
}
