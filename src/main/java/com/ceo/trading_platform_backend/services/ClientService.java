package com.ceo.trading_platform_backend.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ceo.trading_platform_backend.repositories.PortfolioRepository;
import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.Portfolio;
import com.ceo.trading_platform_backend.repositories.OrderRepository;
import com.ceo.trading_platform_backend.repositories.PortfolioRepository;

/**
 * ClientService handles client-specific business logic.
 * All methods operate on data scoped to a single client.
 */
@Service
public class ClientService {
    
    private final PortfolioRepository portfolioRepository;
    private final OrderRepository orderRepository;

    public ClientService(PortfolioRepository portfolioRepository, OrderRepository orderRepository) {
        this.portfolioRepository = portfolioRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Place an order for a client (buy or sell).
     * 
     * Step 1: Validate client exists and is active
     * Step 2: Validate order (symbol exists, quantity positive)
     * Step 3: Create Order record in database
     * Step 4: Update Holding record (create if new position, update if existing)
     * Step 5: Return Order with order_id and status
     */
    @Transactional
    public Order placeOrder(UUID clientId, Order order) {
        return null;
    }

    /**
     * Get current positions for a client.
     * Shows all active holdings across client's portfolios.
     */
    @Transactional(readOnly = true)
    public List<Holding> getPositions(UUID clientId) {
        return List.of();
    }

    /**
     * Get transaction history for a client.
     * Shows all past orders in chronological order (most recent first).
     */
    @Transactional(readOnly = true)
    public List<Order> viewTransactionHistory(UUID clientId) {
        return List.of();
    }

    /**
     * Get a specific order's details and status.
     * Used by client to check order status after placement.
     */
    @Transactional(readOnly = true)
    public Order getOrderStatus(UUID clientId, UUID orderId) {
        return null;
    }

    /**
     * Get all portfolios for a client.
     */
    @Transactional(readOnly = true)
    public List<Portfolio> getPortfolios(UUID clientId) {
        List<Portfolio> portfolios = portfolioRepository.findByClientUserId(clientId);
        if (portfolios == null) return null;
        return portfolios;
    }

}
