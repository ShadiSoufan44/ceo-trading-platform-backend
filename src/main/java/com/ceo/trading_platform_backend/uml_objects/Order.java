package com.ceo.trading_platform_backend.uml_objects;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ceo.trading_platform_backend.uml_objects.Enums.OrderStatus;
import com.ceo.trading_platform_backend.uml_objects.Enums.Side;

public class Order {

    int orderId;
    Client client;
    int portfolioId;
    
    Instrument instrument;
    
    Date createdDate;
    Date resolvedDate;

    List<OrderStatusChange> orderHistory = new ArrayList<>();

    double quantity;
    double quotedPrice;
    Side side;
    double wentUpTooMuchThreshold;


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
        this.orderHistory.add(new OrderStatusChange(
            OrderStatus.PENDING,
            "Order Created",
            createdDate
        ));
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
    public List<OrderStatusChange> getOrderHistory() {
        return orderHistory;
    }
    public void addOrderStatusChange(OrderStatusChange orderStatusChange) {
        orderHistory.add(orderStatusChange);
    }
    public OrderStatusChange getCurrentOrderStatus() {
        return orderHistory.getLast();
    }
}
