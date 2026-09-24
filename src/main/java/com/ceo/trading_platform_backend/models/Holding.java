package com.ceo.trading_platform_backend.models;

import java.util.Date;

import org.springframework.data.annotation.CreatedDate;

import com.ceo.trading_platform_backend.uml_objects.Instrument;
import com.ceo.trading_platform_backend.uml_objects.Order;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity 
@Table (name="holding")
public class Holding {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int ID;

    @CreatedDate 
    private Date dateCreated;

    @ManyToOne 
    private Instrument instrument;

    @OneToOne 
    private Order order;

    private double purchasedPrice;
    private double quantity;
    
    public Holding() {}

    public Holding(Date dateCreated, double purchasedPrice, double quantity, Instrument instrument, Order order) {
        this.dateCreated = dateCreated;
        this.purchasedPrice = purchasedPrice;
        this.quantity = quantity;
        this.instrument = instrument;
    }

    public Instrument getInstrument() {
        return this.instrument;
    }
    public double getQuantity() {
        return this.quantity;
    }
    public Date getDateCreated() {
        return dateCreated;
    }
    public Order getOrder() {
        return order;
    }
    public double getPurchasedPrice() {
        return purchasedPrice;
    }  

}
