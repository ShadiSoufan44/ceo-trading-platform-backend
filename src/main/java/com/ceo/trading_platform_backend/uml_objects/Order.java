package com.ceo.trading_platform_backend.uml_objects;

import java.util.Date;

import com.ceo.trading_platform_backend.uml_objects.Enums.OrderStatus;
import com.ceo.trading_platform_backend.uml_objects.Enums.Side;

public class Order {
    Client client;
    
    Instrument instrument;
    Portfolio portfolio;
    Date createdDate;
    double quantity;
    double quotedPrice;
    Side side;
    double wentUpTooMuchThreshold;

    OrderStatus orderStatus = OrderStatus.PENDING;
    Holding holding = null;
    String message = "";

    public Order(
        Client client, Instrument instrument,
        Portfolio portfolio, Date createdDate, double quantity, 
        double quotedPrice, double wentUpTooMuchThreshold
    ) {
        this.client = client;
        this.instrument = instrument;
        this.portfolio = portfolio;
        this.createdDate = createdDate;
        this.quantity = quantity;
        this.quotedPrice = quotedPrice;
        this.wentUpTooMuchThreshold = wentUpTooMuchThreshold;   
    }

    public Client getClient() {
        return client;
    }
    public Date getCreatedDate() {
        return createdDate;
    }
    public Holding getHolding() {
        return holding;
    }
    public Instrument getInstrument() {
        return instrument;
    }
    public OrderStatus getOrderStatus() {
        return orderStatus;
    }
    public Portfolio getPortfolio() {
        return portfolio;
    }
    public double getQuantity() {
        return quantity;
    }
    public double getQuotedPrice() {
        return quotedPrice;
    }
    public double getWentUpTooMuchThreshold() {
        return wentUpTooMuchThreshold;
    }
    public Side getSide() {
        return side;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
