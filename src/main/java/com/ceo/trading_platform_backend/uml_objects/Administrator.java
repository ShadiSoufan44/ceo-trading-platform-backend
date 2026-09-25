package com.ceo.trading_platform_backend.uml_objects;

import java.time.OffsetDateTime;

/**
 * Represents an Administrator/Ops user - responsible for trade accountability.
 * Persona: David - Head of trading operations, needs complete audit trail,
 * can answer any dispute about what happened and when without engineering.
 */
public class Administrator extends User {

    public Administrator() {
        super();
    }

    public Administrator(String fullName, String email, String password, OffsetDateTime joinDate) {
        super(fullName, email, password, joinDate, "ADMIN");
    }

    // TODO: Administrator-specific capabilities
    // - getAllTrades() - Complete trade history across all clients
    // - auditTradeDisputeTradeLookup(Integer tradeId) - Get exact trade details
    // - viewAuditLog(Integer userId) - See all user actions
    // - suspendUser(Integer userId) - Deactivate accounts
    // - getUserManagement() - Create/update/delete users
    // - systemMetrics() - View overall system health
    // - canViewUser(Integer userId) - Can view any user's data
}
