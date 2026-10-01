package com.ceo.trading_platform_backend.models;

import java.math.BigDecimal;

import com.ceo.trading_platform_backend.uml_objects.Enums.Side;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "\"order\"")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "instr_id")
    private Integer instrumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "side")
    private Side side;

    @Column(name = "portfolio_id")
    private Integer portfolioId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "holding_id")
    private Integer holdingId;

    @Column(name = "quote")
    private BigDecimal quote;

    @Column(name = "final_price")
    private BigDecimal finalPrice;

    @Column(name = "increase_threshold")
    private BigDecimal increaseThreshold;

    @Column(name = "quantity")
    private BigDecimal quantity;

    protected Order() {
    }

    public Order(Integer instrumentId, Side side, Integer portfolioId, Integer userId, Integer holdingId,
            BigDecimal quote, BigDecimal finalPrice, BigDecimal increaseThreshold) {
        this.instrumentId = instrumentId;
        this.side = side;
        this.portfolioId = portfolioId;
        this.userId = userId;
        this.holdingId = holdingId;
        this.quote = quote;
        this.finalPrice = finalPrice;
        this.increaseThreshold = increaseThreshold;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public Integer getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(Integer instrumentId) {
        this.instrumentId = instrumentId;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public Integer getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Integer portfolioId) {
        this.portfolioId = portfolioId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getHoldingId() {
        return holdingId;
    }

    public void setHoldingId(Integer holdingId) {
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
    
    public BigDecimal getQuantity() {
        return quantity;
    }
}