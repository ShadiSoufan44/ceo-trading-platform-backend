package com.ceo.trading_platform_backend.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.enums.Side;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "\"order\"")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "instrument_id")
    private UUID instrumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "side")
    private Side side;

    @Column(name = "portfolio_id")
    private UUID portfolioId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "holding_id")
    private UUID holdingId;

    @Column(name = "quote")
    private BigDecimal quote;

    @Column(name = "final_price")
    private BigDecimal finalPrice;

    @Column(name = "increase_threshold")
    private BigDecimal increaseThreshold;

    @Column(name = "quantity")
    private BigDecimal quantity;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    List<OrderStatusChange> orderHistory = new ArrayList<>();

    protected Order() {
    }

    public Order(UUID instrumentId, Side side, UUID portfolioId, UUID clientId, /*Integer holdingId,*/
            BigDecimal quote/* , BigDecimal finalPrice */, BigDecimal increaseThreshold, BigDecimal quantity) {
        this.instrumentId = instrumentId;
        this.side = side;
        this.portfolioId = portfolioId;
        this.clientId = clientId;
        // this.holdingId = holdingId;
        this.quote = quote;
        // this.finalPrice = finalPrice;
        this.increaseThreshold = increaseThreshold;
        this.quantity = quantity;
        Instant createdDate = Instant.now();
        this.orderHistory.add(new OrderStatusChange(
            OrderStatus.PENDING,
            "Order Created",
            createdDate
        ));
    }

    public void addOrderStatusChange(OrderStatusChange orderStatusChange) {
        orderHistory.add(orderStatusChange);
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(UUID instrumentId) {
        this.instrumentId = instrumentId;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public UUID getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(UUID portfolioId) {
        this.portfolioId = portfolioId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public UUID getHoldingId() {
        return holdingId;
    }

    public void setHoldingId(UUID holdingId) {
        this.holdingId = holdingId;
    }

    public BigDecimal getQuote() {
        return quote;
    }

    public void setQuote(BigDecimal quote) {
        this.quote = quote;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }

    public BigDecimal getIncreaseThreshold() {
        return increaseThreshold;
    }

    public void setIncreaseThreshold(BigDecimal increaseThreshold) {
        this.increaseThreshold = increaseThreshold;
    }

    public Instant getCreatedDate() {
        return this.orderHistory.getFirst().getDate();
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public OrderStatusChange getCurrentOrderStatus() {
        return orderHistory.getLast();
    }

}