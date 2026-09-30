package com.ceo.trading_platform_backend.services;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ceo.trading_platform_backend.dto.PortfolioResponse;
import com.ceo.trading_platform_backend.models.Portfolio;
import com.ceo.trading_platform_backend.repositories.PortfolioRepository;
import com.ceo.trading_platform_backend.uml_objects.Enums.PortfolioType;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class PortfolioServiceTests {
    @Mock 
    private PortfolioRepository portfolioRepository;

    @InjectMocks 
    private PortfolioService portfolioService;

    @BeforeEach 
    void setup() {
        Portfolio mockPortfolio = new Portfolio(PortfolioType.BROKERAGE, new BigDecimal(99));
        when(portfolioRepository.findById(1)).thenReturn(Optional.of(mockPortfolio));
    }
    

    @Test
    void testGetPortfolioById_Success() {
        
        Portfolio result = portfolioService.getPortfolioById(1);
        
        assertThat(result).isNotNull();
        verify(portfolioRepository).findById(1);
    }

    @Test
    void testGetPortfolioById_Fail() {
        when(portfolioRepository.findById(2)).thenReturn(null);

        Portfolio result = portfolioService.getPortfolioById(2);

        assertThat(result).isNull();
        verify(portfolioRepository).findById(2);
    }

    @Test 
    void testGetPortfolioResponseById_Success() {
        Portfolio mockPortfolio = new Portfolio(PortfolioType.BROKERAGE, new BigDecimal(99));
        when(portfolioRepository.findById(1)).thenReturn(Optional.of(mockPortfolio));

        PortfolioResponse result = portfolioService.getPortfolioResponseById(1);

        assertThat(result).isNotNull();
        verify(portfolioRepository).findById(1)
    }

}
