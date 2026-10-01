package com.ceo.trading_platform_backend.uml_objects;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

/**
 * Represents an Administrator/Ops user 
 */
@Entity 
@DiscriminatorValue("ADMIN")
public class Administrator extends User {

    @OneToMany private Set<AuditLog> auditLogs = new HashSet<>();
    
    public Administrator() {
        super();
    }

    public Administrator(String fullName, String email, String password, OffsetDateTime joinDate) {
        super(fullName, email, password, joinDate, "ADMIN");
    }

    public Set<AuditLog> getAuditLogs() {
        return this.auditLogs;
    }

    public void setAuditLogs(Set<AuditLog> auditLogs) {
        this.auditLogs = auditLogs;
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
