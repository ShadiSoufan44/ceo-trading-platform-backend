package com.ceo.trading_platform_backend.uml_objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.math.BigDecimal;

import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.uml_objects.Enums.OrderStatus;
import com.ceo.trading_platform_backend.uml_objects.Enums.Side;

public class Order extends com.ceo.trading_platform_backend.models.Order {

    Client client;
    Instrument instrument;
    Date createdDate;
    Date resolvedDate;

    List<OrderStatusChange> orderHistory = new ArrayList<>();

    double quantity;
    double quotedPrice;
    double wentUpTooMuchThreshold;
    String message;


    public Order(
        Client client, Instrument instrument,
        int portfolioId, Date createdDate, double quantity, 
        double quotedPrice, double wentUpTooMuchThreshold
    ) {
        super();
        this.client = client;
        this.instrument = instrument;
        this.createdDate = createdDate;
        this.quantity = quantity;
        this.quotedPrice = quotedPrice;
        this.wentUpTooMuchThreshold = wentUpTooMuchThreshold;   
        setPortfolioId(portfolioId);
        setQuote(BigDecimal.valueOf(quotedPrice));
        setIncreaseThreshold(BigDecimal.valueOf(wentUpTooMuchThreshold));
        if (client != null) {
            setUserId(client.userID);
        }
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

    public int getPortfolioID() {
        return getPortfolioId() == null ? 0 : getPortfolioId();
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

    public List<OrderStatusChange> getOrderHistory() {
        return Collections.unmodifiableList(orderHistory);
    }

    public void addOrderStatusChange(OrderStatusChange orderStatusChange) {
        orderHistory.add(orderStatusChange);
    }

    public OrderStatusChange getCurrentOrderStatus() {
        return orderHistory.getLast();
    }
   
}
