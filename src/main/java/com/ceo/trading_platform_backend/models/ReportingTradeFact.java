package com.ceo.trading_platform_backend.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reporting_trade_fact")
public class ReportingTradeFact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "trade_fact_id")
    private UUID tradeFactId;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "client_segment", nullable = false)
    private String clientSegment;

    @Column(name = "instrument_id", nullable = false)
    private UUID instrumentId;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "side", nullable = false)
    private String side;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "notional", nullable = false)
    private BigDecimal notional;

    protected ReportingTradeFact() {
    }

    public UUID getTradeFactId() {
        return tradeFactId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public UUID getClientId() {
        return clientId;
    }

    public String getClientSegment() {
        return clientSegment;
    }

    public UUID getInstrumentId() {
        return instrumentId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSide() {
        return side;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getNotional() {
        return notional;
    }
}
