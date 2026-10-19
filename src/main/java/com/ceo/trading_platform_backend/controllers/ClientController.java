package com.ceo.trading_platform_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ceo.trading_platform_backend.services.ClientService;
import com.ceo.trading_platform_backend.dto.PortfolioResponse;
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
    public ResponseEntity<List<PortfolioResponse>> getPortfolios(@PathVariable UUID userId) {
        List<Portfolio> portfolios = clientService.getPortfolios(userId);
        List<PortfolioResponse> response = portfolios.stream()
            .map(this::toPortfolioResponse)
            .toList();
        return ResponseEntity.ok(response);
    }

    private PortfolioResponse toPortfolioResponse(Portfolio portfolio) {
        List<UUID> holdingIds = portfolio.getHoldings().stream()
            .map(holding -> holding.getID())
            .toList();
        return new PortfolioResponse(
            portfolio.getPortfolioId(),
            holdingIds,
            portfolio.getType()
        );
    }
}

