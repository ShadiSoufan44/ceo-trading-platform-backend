package com.ceo.trading_platform_backend.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ceo.trading_platform_backend.dto.HoldingResponse;
import com.ceo.trading_platform_backend.dto.PortfolioResponse;
import com.ceo.trading_platform_backend.services.PortfolioService;

import java.util.List;

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

    // TODO: move this to a ClientController.java
    @GetMapping("/getall/{client_id}")
    public ResponseEntity<List<PortfolioResponse>> getPortfoliosByClientId(
        @PathVariable int clientId
    ) {
        List<PortfolioResponse> response = service.getPortfoliosByClientId(clientId);
        if (response == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{portfolio_id}/buying_power")
    public ResponseEntity<Double> getBuyingPowerByPortfolioId(
        @PathVariable int portfolioId
    ) {
        Double response = service.getBuyingPowerByPortfolioId(portfolioId);
        if (response == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{portfolio_id}/holdings")
    public ResponseEntity<List<HoldingResponse>> getHoldingsByPortfolioId(
        @PathVariable int portfolioId
    ) {
        List<HoldingResponse> response = service.getHoldingsByPortfolioId(portfolioId);
        if (response == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{portfolio_id}")
    public ResponseEntity<PortfolioResponse> getPortfolioById(@PathVariable int portfolioId) {
        PortfolioResponse response = service.getPortfolioResponseById(portfolioId);
        if (response == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(response);
    }
    
    
}
