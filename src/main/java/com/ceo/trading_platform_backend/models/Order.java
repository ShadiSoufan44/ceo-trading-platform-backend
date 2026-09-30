package com.ceo.trading_platform_backend.models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ceo.trading_platform_backend.uml_objects.OrderStatusChange;
import com.ceo.trading_platform_backend.uml_objects.Enums.OrderStatus;
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

    // not a column in table
    List<OrderStatusChange> orderHistory = new ArrayList<>();
    


    protected Order() {
    }

    public Order(Integer instrumentId, Side side, Integer portfolioId, Integer userId, /*Integer holdingId,*/
            BigDecimal quote/* , BigDecimal finalPrice */, BigDecimal increaseThreshold, BigDecimal quantity) {
        this.instrumentId = instrumentId;
        this.side = side;
        this.portfolioId = portfolioId;
        this.userId = userId;
        // this.holdingId = holdingId;
        this.quote = quote;
        // this.finalPrice = finalPrice;
        this.increaseThreshold = increaseThreshold;
        this.quantity = quantity;
        Date createdDate = new Date(); //not sure if this is correct place ot get date
        this.orderHistory.add(new OrderStatusChange(
            OrderStatus.PENDING,
            "Order Created",
            createdDate
        ));
    }

    public void addOrderStatusChange(OrderStatusChange orderStatusChange) {
        orderHistory.add(orderStatusChange);
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

    public Date getCreatedDate() {
        return this.orderHistory.getFirst().getDate();
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public OrderStatusChange getCurrentOrderStatus() {
        return orderHistory.getLast();
    }

}