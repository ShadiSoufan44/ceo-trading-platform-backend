package com.ceo.trading_platform_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ceo.trading_platform_backend.services.ClientService;
import com.ceo.trading_platform_backend.models.Portfolio;


@RestController 
@RequestMapping("/api/users/{userId}")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    /**
     * Get all portfolios for a client.
     */
    @GetMapping("/portfolios")
    @PreAuthorize("@userService.isOwnProfile(#userId)")
    public ResponseEntity<List<Portfolio>> getPortfolios(@PathVariable UUID userId) {
        List<Portfolio> portfolios = clientService.getPortfolios(userId);
        return ResponseEntity.ok(portfolios);
    }
}

