package com.ceo.trading_platform_backend.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController 
@RequestMapping("/api/portfolio")
public class PortfolioController {
    
    public PortfolioController() {

    }

    @GetMapping("/getall/{client_id}")
    public String getPortfoliosByClientId(@PathVariable int clientId) {
        return new String();
    }
    
    @GetMapping("/{portfolio_id}")
    public String getPortfolioById(@PathVariable int portfolioId) {
        return new String();
    }
    
    @GetMapping("/buying_power/{portfolio_id}")
    public String getBuyingPowerByPortfolioId(@PathVariable int portfolioId) {
        return new String();
    }

    @GetMapping("/holdings/{portfolio_id}")
    public String getHoldingsByPortfolioId(@PathVariable int portfolioId) {
        return new String();
    }
    
    
}
