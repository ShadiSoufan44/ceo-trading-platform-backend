package com.ceo.trading_platform_backend.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ceo.trading_platform_backend.dto.HoldingSummaryResponse;
import com.ceo.trading_platform_backend.dto.PerformanceResponse;
import com.ceo.trading_platform_backend.dto.PortfolioResponse;
import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Portfolio;
import com.ceo.trading_platform_backend.services.PortfolioService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController 
@RequestMapping("/api/portfolio")
public class PortfolioController {
    
    PortfolioService service;

    public PortfolioController(PortfolioService portfolioService) {
        this.service = portfolioService;
    }

    @GetMapping("/{portfolioId}/buying_power")
    public ResponseEntity<BigDecimal> getBuyingPowerByPortfolioId(
        @PathVariable UUID portfolioId
    ) {
        BigDecimal response = service.getBuyingPowerByPortfolioId(portfolioId);
        if (response == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{portfolioId}/total_value")
    public ResponseEntity<BigDecimal> getTotalValueByPortfolioId(
        @PathVariable UUID portfolioId
    ) {
        BigDecimal response = service.getTotalValueByPortfolioId(portfolioId);
        if (response == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{portfolioId}/holdings")
    public ResponseEntity<List<HoldingSummaryResponse>> getHoldingsByPortfolioId(
        @PathVariable UUID portfolioId
    ) {
        return ResponseEntity.ok(service.getHoldingSummariesByPortfolioId(portfolioId));
    }
    
    @GetMapping("/{portfolioId}")
    public ResponseEntity<PortfolioResponse> getPortfolioById(@PathVariable UUID portfolioId) {
        Portfolio portfolio = service.getPortfolioById(portfolioId);
        if (portfolio == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        PortfolioResponse response = createPortfolioResponse(portfolio);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{portfolioId}/performance/{timeframe}")
    public ResponseEntity<List<PerformanceResponse>> getPortfolioPerformance(@PathVariable UUID portfolioId, @PathVariable String timeframe) {
        List<PerformanceResponse> response = service.getPortfolioPerformance(portfolioId, timeframe);
        if (response == null || response.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(response);
    }

    private PortfolioResponse createPortfolioResponse(Portfolio portfolio) {
        if (portfolio == null) return null;

        List<UUID> holdingIds = new ArrayList<>();

        for (Holding holding : portfolio.getHoldings()) {
            holdingIds.add(holding.getID());
        }
        return new PortfolioResponse(
            portfolio.getPortfolioId(), holdingIds, portfolio.getType()
        );
    }

}
