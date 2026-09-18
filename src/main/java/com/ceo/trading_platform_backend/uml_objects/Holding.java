package com.ceo.trading_platform_backend.uml_objects;

import java.util.Date;

public class Holding {
    
    Date dateCreated;
    double purchasePrice;
    double quantity;
    Instrument instrument;
    Order order;
    int ID;

    public Holding(Date dateCreated, double purchasePrice, double quantity, Instrument instrument, Order order) {
        this.dateCreated = dateCreated;
        this.purchasePrice = purchasePrice;
        this.quantity = quantity;
        this.instrument = instrument;
        //this.id = newUID();
    }

    Instrument getInstrument() {
        return this.instrument;
    }

    double getQuantity() {
        return this.quantity;
    }

    

}
