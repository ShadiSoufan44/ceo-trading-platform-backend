package com.ceo.trading_platform_backend.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ceo.trading_platform_backend.repositories.OrderRepository;
import com.ceo.trading_platform_backend.repositories.PortfolioRepository;
import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.Portfolio;

/**
 * ClientService handles client-specific business logic.
 * All methods operate on data scoped to a single client.
 */
@Service
public class ClientService {
    
    private final OrderRepository orderRepository;
    private final PortfolioRepository portfolioRepository;

    public ClientService(OrderRepository orderRepository, PortfolioRepository portfolioRepository) {
        this.orderRepository = orderRepository;
        this.portfolioRepository = portfolioRepository;
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
    public Order placeOrder(Integer clientId, Order order) {
        return null;
    }

    /**
     * Get current positions for a client.
     * Shows all active holdings across client's portfolios.
     */
    @Transactional(readOnly = true)
    public List<Holding> getPositions(Integer clientId) {
        return List.of();
    }

    /**
     * Get transaction history for a client.
     * Shows all past orders in chronological order (most recent first).
     */
    @Transactional(readOnly = true)
    public List<Order> viewTransactionHistory(Integer clientId) {
        return List.of();
    }

    /**
     * Get a specific order's details and status.
     * Used by client to check order status after placement.
     */
    @Transactional(readOnly = true)
    public Order getOrderStatus(Integer clientId, Integer orderId) {
        return null;
    }

    /**
     * Get all portfolios for a client.
     */
    @Transactional(readOnly = true)
    public List<Portfolio> getPortfolios(Integer clientId) {
        return List.of();
    }

    /**
     * Get current stock quote (price) from market API.
     * Used by client to check current prices before placing orders.
     */
    @Transactional(readOnly = true)
    public Object getStockQuote(String symbol) {
        return null;
    }
}
