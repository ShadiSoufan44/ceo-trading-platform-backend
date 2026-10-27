package com.ceo.trading_platform_backend.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.services.InstrumentService;
import com.ceo.trading_platform_backend.services.MarketService;

import com.ceo.trading_platform_backend.dto.PerformanceResponse;

@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {

        this.instrumentService = instrumentService;
    }


    @GetMapping("/{symbol}/{type}/fullname")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public ResponseEntity<Object> getInstrumentFullName(
        @PathVariable String symbol,
        @PathVariable InstrumentType type
    ) {
        Instrument instrument = instrumentService.getOrInsertInstrumentBySymbolAndType(symbol, type);
        String fullName = instrument.getFullName();
        return ResponseEntity.ok(fullName);
    }
}

