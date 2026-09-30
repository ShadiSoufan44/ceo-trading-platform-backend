package com.ceo.trading_platform_backend.services;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ceo.trading_platform_backend.dto.HoldingResponse;
import com.ceo.trading_platform_backend.dto.PortfolioResponse;
import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Portfolio;
import com.ceo.trading_platform_backend.repositories.PortfolioRepository;
import com.ceo.trading_platform_backend.uml_objects.Enums.InstrumentType;
import com.ceo.trading_platform_backend.uml_objects.Enums.PortfolioType;
import com.ceo.trading_platform_backend.uml_objects.Instrument;
import com.ceo.trading_platform_backend.uml_objects.Order;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioService Tests")
public class PortfolioServiceTests {
    @Mock 
    private PortfolioRepository portfolioRepository;

    @InjectMocks 
    private PortfolioService portfolioService;

    private Instrument usdInstrument;
    private Instrument stockInstrument;

    @BeforeEach 
    void setup() {
        usdInstrument = new Instrument("USD", InstrumentType.CASH, "United States Dollar");
        stockInstrument = new Instrument("AAPL", InstrumentType.EQUITY, "Apple Inc.");
    }

    private Portfolio createTestPortfolio(int id, PortfolioType type) {
        Portfolio portfolio = new Portfolio(type, BigDecimal.ZERO);
        return portfolio;
    }

    private Order createTestOrder(int orderId) {
        Order order = new Order(
            null, 
            null, 
            0, 
            null, 
            0, 
            0, 
            0
        );
        return order;
    }

    private Holding createCashHolding(BigDecimal quantity) {
        return new Holding(
            new Date(),
            BigDecimal.ONE,
            quantity,
            usdInstrument,
            createTestOrder(0)
        );
    }

    private Holding createStockHolding(BigDecimal quantity, BigDecimal price) {
        return new Holding(
            new Date(),
            price,
            quantity,
            stockInstrument,
            createTestOrder(1)
        );
    }

    @Nested
    @DisplayName("getPortfolioById()")
    class GetPortfolioByIdTests {
        @Test
        @DisplayName("Should return correct portfolio when it exists")
        void shouldReturnPortfolioWhenExists() {
            Portfolio expected = createTestPortfolio(1, PortfolioType.BROKERAGE);
            when(portfolioRepository.findById(1)).thenReturn(Optional.of(expected));

            Portfolio result = portfolioService.getPortfolioById(1);

            assertThat(result).isNotNull().isEqualTo(expected);
            verify(portfolioRepository).findById(1);
        }

        @Test
        @DisplayName("Should return null when portfolio ID doesn't exist")
        void shouldReturnNullWhenNotExists() {
            when(portfolioRepository.findById(999)).thenReturn(Optional.empty());

            Portfolio result = portfolioService.getPortfolioById(999);

            assertThat(result).isNull();
            verify(portfolioRepository).findById(999);
        }
    }

    @Nested
    @DisplayName("getPortfolioResponseById()")
    class GetPortfolioResponseByIdTests {
        @Test
        @DisplayName("Should return correct response when portfolio exists")
        void shouldReturnResponseWhenExists() {
            Portfolio portfolio = createTestPortfolio(1, PortfolioType.BROKERAGE);
            when(portfolioRepository.findById(1)).thenReturn(Optional.of(portfolio));

            PortfolioResponse result = portfolioService.getPortfolioResponseById(1);

            assertThat(result).isNotNull();
            assertThat(result.ID()).isEqualTo(portfolio.getPortfolioId());
            assertThat(result.type()).isEqualTo(PortfolioType.BROKERAGE);
            verify(portfolioRepository).findById(1);
        }

        @Test
        @DisplayName("Should return null when portfolio ID doesn't exist")
        void shouldReturnNullWhenNotExists() {
            when(portfolioRepository.findById(999)).thenReturn(Optional.empty());

            PortfolioResponse result = portfolioService.getPortfolioResponseById(999);

            assertThat(result).isNull();
            verify(portfolioRepository).findById(999);
        }
    }

    @Nested
    @DisplayName("getBuyingPowerByPortfolioId()")
    class GetBuyingPowerByPortfolioIdTests {
        @Test
        @DisplayName("Should return null when portfolio ID doesn't exist")
        void shouldReturnNullWhenPortfolioNotExists() {
            when(portfolioRepository.findById(999)).thenReturn(Optional.empty());

            BigDecimal result = portfolioService.getBuyingPowerByPortfolioId(999);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return 0 when portfolio has no cash holdings")
        void shouldReturnZeroWhenNoCashHoldings() {
            Portfolio portfolio = createTestPortfolio(1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createStockHolding(new BigDecimal("10"), new BigDecimal("150")));
            
            when(portfolioRepository.findById(1)).thenReturn(Optional.of(portfolio));

            BigDecimal result = portfolioService.getBuyingPowerByPortfolioId(1);

            assertThat(result).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return correct value with 1 cash holding")
        void shouldReturnCorrectValueWithOneCashHolding() {
            Portfolio portfolio = createTestPortfolio(1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createCashHolding(new BigDecimal("5000")));
            
            when(portfolioRepository.findById(1)).thenReturn(Optional.of(portfolio));

            BigDecimal result = portfolioService.getBuyingPowerByPortfolioId(1);

            assertThat(result).isEqualTo(new BigDecimal("5000"));
        }

        @Test
        @DisplayName("Should return correct value with multiple cash holdings")
        void shouldReturnCorrectValueWithMultipleCashHoldings() {
            Portfolio portfolio = createTestPortfolio(1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createCashHolding(new BigDecimal("3000")));
            portfolio.addHolding(createCashHolding(new BigDecimal("2000")));
            
            when(portfolioRepository.findById(1)).thenReturn(Optional.of(portfolio));

            BigDecimal result = portfolioService.getBuyingPowerByPortfolioId(1);

            assertThat(result).isEqualTo(new BigDecimal("5000"));
        }
    }

    @Nested
    @DisplayName("getHoldingsByPortfolioId()")
    class GetHoldingsByPortfolioIdTests {
        @Test
        @DisplayName("Should return null when portfolio ID doesn't exist")
        void shouldReturnNullWhenPortfolioNotExists() {
            when(portfolioRepository.findById(999)).thenReturn(Optional.empty());

            List<HoldingResponse> result = portfolioService.getHoldingsByPortfolioId(999);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return empty list when portfolio has no holdings")
        void shouldReturnEmptyListWhenNoHoldings() {
            Portfolio portfolio = createTestPortfolio(1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            
            when(portfolioRepository.findById(1)).thenReturn(Optional.of(portfolio));

            List<HoldingResponse> result = portfolioService.getHoldingsByPortfolioId(1);

            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should return list with 1 holding")
        void shouldReturnListWithOneHolding() {
            Portfolio portfolio = createTestPortfolio(1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createCashHolding(new BigDecimal("1000")));
            
            when(portfolioRepository.findById(1)).thenReturn(Optional.of(portfolio));

            List<HoldingResponse> result = portfolioService.getHoldingsByPortfolioId(1);

            assertThat(result)
                .isNotNull()
                .hasSize(1)
                .allMatch(h -> h != null);
        }

        @Test
        @DisplayName("Should return list with multiple holdings")
        void shouldReturnListWithMultipleHoldings() {
            Portfolio portfolio = createTestPortfolio(1, PortfolioType.BROKERAGE);
            portfolio.getHoldings().clear();
            portfolio.addHolding(createCashHolding(new BigDecimal("2000")));
            portfolio.addHolding(createStockHolding(new BigDecimal("10"), new BigDecimal("150")));
            portfolio.addHolding(createCashHolding(new BigDecimal("1000")));
            
            when(portfolioRepository.findById(1)).thenReturn(Optional.of(portfolio));

            List<HoldingResponse> result = portfolioService.getHoldingsByPortfolioId(1);

            assertThat(result)
                .isNotNull()
                .hasSize(3)
                .allMatch(h -> h != null);
        }
    }
}
