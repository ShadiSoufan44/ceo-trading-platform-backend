package com.ceo.trading_platform_backend.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ceo.trading_platform_backend.services.AnalystService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController 
@RequestMapping("/api/candles/{symbol}")
public class AnalystController {
    
    private final AnalystService analystService;

    public AnalystController(AnalystService analystService) {
        this.analystService = analystService;
    }
    
    @GetMapping("/api/candles")
    public String getMethodName(@RequestParam String param) {
        return new String();
    }
    
}
