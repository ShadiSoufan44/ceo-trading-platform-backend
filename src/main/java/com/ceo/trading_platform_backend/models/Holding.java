package com.ceo.trading_platform_backend.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity 
@Table (name="holding")
public class Holding {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "holding_id")
    private UUID ID;

    @CreatedDate 
    @Column(name = "order_date")
    private Instant dateCreated;

    @ManyToOne 
    @JoinColumn(name = "instrument_id")
    private Instrument instrument;

    @OneToOne 
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "purchased_price")
    private BigDecimal purchasedPrice;

    @Column(name = "quantity")
    private BigDecimal quantity;

    @ManyToOne 
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;
    
    public Holding() {}

    public Holding(
        Instant dateCreated, 
        BigDecimal purchasedPrice, 
        BigDecimal quantity, 
        Instrument instrument, 
        Order order,
        Portfolio portfolio
    ) {
        this.dateCreated = dateCreated;
        this.purchasedPrice = purchasedPrice;
        this.quantity = quantity;
        this.instrument = instrument;
        this.order = order;
        this.portfolio = portfolio;
    }

    public Instrument getInstrument() {
        return this.instrument;
    }
    public BigDecimal getQuantity() {
        return this.quantity;
    }
    public Instant getDateCreated() {
        return dateCreated;
    }
    public Order getOrder() {
        return order;
    }
    public BigDecimal getPurchasedPrice() {
        return purchasedPrice;
    }  
    public UUID getID() {
        return ID;
    }

}
