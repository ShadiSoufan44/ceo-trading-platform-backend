package com.ceo.trading_platform_backend.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ceo.trading_platform_backend.dto.HoldingResponse;
import com.ceo.trading_platform_backend.dto.PortfolioResponse;
import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Portfolio;
import com.ceo.trading_platform_backend.services.PortfolioService;

import java.math.BigDecimal;
import java.util.ArrayList;
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
    // @GetMapping("/getall/{client_id}")
    // public ResponseEntity<List<PortfolioResponse>> getPortfoliosByClientId(
    //     @PathVariable int clientId
    // ) {
    //     List<Portfolio> portfolios = service.getPortfoliosByClientId(clientId);
    //     if (portfolios == null) {
    //         return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    //     }
    //     List<PortfolioResponse> response = new ArrayList<>();
    //     for (Portfolio portfolio : portfolios) {
    //         response.add(createPortfolioResponse(portfolio));
    //     }
        
    //     return ResponseEntity.ok(response);
    // }

    @GetMapping("/{portfolioId}/buying_power")
    public ResponseEntity<BigDecimal> getBuyingPowerByPortfolioId(
        @PathVariable int portfolioId
    ) {
        BigDecimal response = service.getBuyingPowerByPortfolioId(portfolioId);
        if (response == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{portfolioId}/total_value")
    public ResponseEntity<BigDecimal> getTotalValueByPortfolioId(
        @PathVariable int portfolioId
    ) {
        BigDecimal response = service.getTotalValueByPortfolioId(portfolioId);
        if (response == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(response);
    }
    

    @GetMapping("/{portfolioId}/holdings")
    public ResponseEntity<List<HoldingResponse>> getHoldingsByPortfolioId(
        @PathVariable int portfolioId
    ) {
        List<Holding> holdings = service.getHoldingsByPortfolioId(portfolioId);
        if (holdings == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        List<HoldingResponse> response = new ArrayList<>();
        for (Holding holding : holdings) {
            response.add(createHoldingResponse(holding));
        }
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{portfolioId}")
    public ResponseEntity<PortfolioResponse> getPortfolioById(@PathVariable int portfolioId) {
        Portfolio portfolio = service.getPortfolioById(portfolioId);
        if (portfolio == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        PortfolioResponse response = createPortfolioResponse(portfolio);
        return ResponseEntity.ok(response);
    }

    private PortfolioResponse createPortfolioResponse(Portfolio portfolio) {
        if (portfolio == null) return null;

        List<Integer> holdingIds = new ArrayList<>();

        for (Holding holding : portfolio.getHoldings()) {
            holdingIds.add(holding.getID());
        }
        return new PortfolioResponse(
            portfolio.getPortfolioId(), holdingIds, portfolio.getType()
        );
    }

    private HoldingResponse createHoldingResponse(Holding holding) {
        if (holding == null) return null;
        return new HoldingResponse(
            holding.getID(),
            holding.getDateCreated(), 
            holding.getInstrument().getID(),
            holding.getOrder().getOrderId(),
            holding.getPurchasedPrice(),
            holding.getQuantity()
        );
    }
}
