package com.ceo.trading_platform_backend.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Column(name = "add_instrument_id")
    private UUID addInstrumentId;

    @Column(name = "subtract_instrument_id")
    private UUID subtractInstrumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "side")
    private Side side;

    @Column(name = "portfolio_id")
    private UUID portfolioId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "quote")
    private BigDecimal quote;

    @Column(name = "change_threshold")
    private BigDecimal changeThreshold;

    @Column(name = "quantity")
    private BigDecimal quantity;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    List<OrderStatusChange> orderHistory = new ArrayList<>();

    @Column(name = "market")
    private String market;

    protected Order() {
    }

    public Order(UUID addInstrumentId, UUID subtractInstrumentId, 
                Side side, UUID portfolioId, UUID clientId,
                BigDecimal quote, BigDecimal changeThreshold, BigDecimal quantity, String market) {

        this.addInstrumentId = addInstrumentId;
        this.subtractInstrumentId = subtractInstrumentId;
        this.side = side;
        this.portfolioId = portfolioId;
        this.clientId = clientId;
        this.quote = quote;
        this.changeThreshold = changeThreshold;
        this.quantity = quantity;
        this.market = market;
        // this.orderHistory.add(new OrderStatusChange(
        //     OrderStatus.PENDING,
        //     "Order Created",
        //     createdDate
        // ));
    }

    public void addOrderStatusChange(OrderStatusChange orderStatusChange) {
        orderHistory.add(orderStatusChange);
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getAddInstrumentId() {
        return addInstrumentId;
    }

    public UUID getSubtractInstrumentId() {
        return subtractInstrumentId;
    }

    public void setAddInstrumentId(UUID id) {
        this.addInstrumentId = id;
    }

    public void setSubtractInstrumentId(UUID id) {
        this.subtractInstrumentId = id;
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

    public BigDecimal getQuote() {
        return quote;
    }

    public void setQuote(BigDecimal quote) {
        this.quote = quote;
    }

    public BigDecimal getChangeThreshold() {
        return changeThreshold;
    }

    public void setChangeThreshold(BigDecimal changeThreshold) {
        this.changeThreshold = changeThreshold;
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

    public String getMarket() {
        return market;
    }

}