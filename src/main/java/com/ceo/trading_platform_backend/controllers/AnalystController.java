package com.ceo.trading_platform_backend.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ceo.trading_platform_backend.services.AnalystService;


@RestController 
@RequestMapping("/api/analytics")
public class AnalystController {
    
    private final AnalystService analystService;

    public AnalystController(AnalystService analystService) {
        this.analystService = analystService;
    }
    
    /**
     * Get monthly trading volumes by instrument.
     * Only accessible to ANALYST role (Priya).
     */
    @GetMapping("/monthly-volumes")
    @PreAuthorize("hasRole('ANALYST')")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyVolumes() {
        return ResponseEntity.ok(analystService.generateVolumeReport());
    }
    
    /**
     * Get monthly client activity report.
     * Shows trade counts and volume by client per month.
     * Only accessible to ANALYST role (Priya).
     */
    @GetMapping("/client-activity")
    @PreAuthorize("hasRole('ANALYST')")
    public ResponseEntity<List<Map<String, Object>>> getClientActivity() {
        return ResponseEntity.ok(analystService.generateActivityReport());
    }
}
