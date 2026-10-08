package com.ceo.trading_platform_backend.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ceo.trading_platform_backend.models.Portfolio;
import com.ceo.trading_platform_backend.repositories.OrderRepository;
import com.ceo.trading_platform_backend.repositories.PortfolioRepository;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    private ClientService clientService;

    @BeforeEach
    void setUp() {
        clientService = new ClientService(portfolioRepository, orderRepository);
    }

    @Test
    void testGetPortfoliosCallsRepositoryAndReturnsResult() {
        // Arrange
        UUID clientId = UUID.randomUUID();
        Portfolio portfolio = new Portfolio();
        List<Portfolio> expectedList = List.of(portfolio);
        
        when(portfolioRepository.findByClientUserId(clientId)).thenReturn(expectedList);

        // Act
        List<Portfolio> result = clientService.getPortfolios(clientId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(portfolio, result.get(0));
        verify(portfolioRepository).findByClientUserId(clientId);
    }
}
