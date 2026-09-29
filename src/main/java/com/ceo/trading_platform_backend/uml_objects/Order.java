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
    double increaseThreshold;


    public Order(
        Client client, Instrument instrument,
        int portfolioId, Date createdDate, double quantity, 
        double quotedPrice, double increaseThreshold
    ) {
        this.client = client;
        this.instrument = instrument;
        this.portfolioId = portfolioId;
        this.createdDate = createdDate;
        this.quantity = quantity;
        this.quotedPrice = quotedPrice;
        this.increaseThreshold = increaseThreshold;   
        this.orderHistory.add(new OrderStatusChange(
            OrderStatus.PENDING,
            "Order Created",
            createdDate
        ));
    }

    public int getId() {
        return orderId;
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
    public int getPortfolioID() {
        return portfolioId;
    }
    public double getQuantity() {
        return quantity;
    }
    public double getQuotedPrice() {
        return quotedPrice;
    }
    public double getIncreaseThreshold() {
        return increaseThreshold;
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
