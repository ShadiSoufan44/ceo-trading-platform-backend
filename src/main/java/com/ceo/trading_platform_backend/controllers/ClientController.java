package com.ceo.trading_platform_backend.controllers;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ceo.trading_platform_backend.services.ClientService;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Portfolio;


@RestController 
@RequestMapping("/api/users/{userId}")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    /**
     * Place a new order for a client.
     */
    @PostMapping("/orders")
    @PreAuthorize("@userService.isOwnProfile(#userId)")
    public ResponseEntity<Order> placeOrder(
            @PathVariable Integer userId,
            @Valid @RequestBody Order order) {
        Order placedOrder = clientService.placeOrder(userId, order);
        return ResponseEntity.status(HttpStatus.CREATED).body(placedOrder);
    }

    /**
     * Get transaction history (all orders) for a client.
     */
    @GetMapping("/orders")
    @PreAuthorize("@userService.isOwnProfile(#userId)")
    public ResponseEntity<List<Order>> viewTransactionHistory(@PathVariable Integer userId) {
        List<Order> transactions = clientService.viewTransactionHistory(userId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get current positions (holdings) for a client.
     */
    @GetMapping("/positions")
    @PreAuthorize("@userService.isOwnProfile(#userId)")
    public ResponseEntity<List<Holding>> getPositions(@PathVariable Integer userId) {
        List<Holding> positions = clientService.getPositions(userId);
        return ResponseEntity.ok(positions);
    }

    /**
     * Get a specific order's status and details.
     * Clients can only view their own orders.
     */
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("@userService.isOwnProfile(#userId)")
    public ResponseEntity<Order> getOrderStatus(
            @PathVariable Integer userId,
            @PathVariable Integer orderId) {
        Order order = clientService.getOrderStatus(userId, orderId);
        return ResponseEntity.ok(order);
    }

    /**
     * Get all portfolios for a client.
     */
    @GetMapping("/portfolios")
    @PreAuthorize("@userService.isOwnProfile(#userId)")
    public ResponseEntity<List<Portfolio>> getPortfolios(@PathVariable Integer userId) {
        List<Portfolio> portfolios = clientService.getPortfolios(userId);
        return ResponseEntity.ok(portfolios);
    }
}

