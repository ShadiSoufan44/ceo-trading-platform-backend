package com.ceo.trading_platform_backend.uml_objects;

import java.time.OffsetDateTime;

/**
 * Represents an Analyst user - commercial analyst for reporting and insights.
 * Persona: Priya - Reports monthly on trading volumes and client activity,
 * needs reliable historical data, queries cannot compete with live trading.
 */
public class Analyst extends User {

    public Analyst() {
        super();
    }

    public Analyst(String fullName, String email, String password, OffsetDateTime joinDate) {
        super(fullName, email, password, joinDate, "ANALYST");
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
