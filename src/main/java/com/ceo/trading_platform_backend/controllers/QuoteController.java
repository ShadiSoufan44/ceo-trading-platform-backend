package com.ceo.trading_platform_backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ceo.trading_platform_backend.services.ClientService;

/**
 * QuoteController handles stock quote requests.
 */
@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final ClientService clientService;

    public QuoteController(ClientService clientService) {
        this.clientService = clientService;
    }

    /**
     * Get current stock quote (price) for a symbol.
     * Available to all authenticated users.
     */
    @GetMapping("/{symbol}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> getStockQuote(@PathVariable String symbol) {
        Object quote = clientService.getStockQuote(symbol);
        return ResponseEntity.ok(quote);
    }
}
