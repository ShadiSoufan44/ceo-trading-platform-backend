package com.ceo.trading_platform_backend.models;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import com.ceo.trading_platform_backend.uml_objects.Report;

/**
 * Represents an Analyst user - commercial analyst for reporting and insights.
 * Persona: Priya - Reports monthly on trading volumes and client activity,
 * needs reliable historical data, queries cannot compete with live trading.
 */
@DiscriminatorValue("ANALYST")
// @Entity 
public class Analyst extends User {

    @OneToMany private Set<Report> reports = new HashSet<>();

    public Analyst() {
        super();
    }

    public Analyst(String fullName, String email, String password, Instant joinDate) {
        super(fullName, email, password, joinDate, "ANALYST");
    }

    public Set<Report> getReports() {
        return this.reports;
    }

    public void setReports(Set<Report> reports) {
        this.reports = reports;
    }

    // TODO: Analyst-specific capabilities
    // - getHistoricalData(DateRange range) - Query historical trades
    // - generateVolumeReport(Month month) - Trading volume metrics
    // - generateActivityReport(Month month) - Client activity metrics
    // - canViewTradeHistory() - Read-only access to all trades
    // - canViewAggregatedMetrics() - High-level statistics only
    // - cannotPlaceOrders() - No execution capability
    // - canViewUser(Integer userId) - Can view any user's aggregated data (no PII)
}
