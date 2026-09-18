package com.ceo.trading_platform_backend.uml_objects;

import java.util.Date;

import com.ceo.trading_platform_backend.uml_objects.Enums.OrderStatus;
import com.ceo.trading_platform_backend.uml_objects.Enums.Side;

public class Order {

    int orderId;
    Client client;
    int portfolioId;
    
    Instrument instrument;
    
    Date createdDate;
    Date resolvedDate;
    // TODO Idea: OrderStatusChange class, with list of (status, message, date) and 
    // current state would be latest date's status and message


    double quantity;
    double quotedPrice;
    Side side;
    double wentUpTooMuchThreshold;

    OrderStatus orderStatus = OrderStatus.PENDING;
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
