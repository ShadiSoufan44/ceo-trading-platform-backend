package com.ceo.trading_platform_backend.uml_objects;

public class Holding {
    
    String dateCreated;
    double purchasePrice;
    int quantity;
    Instrument instrument;
    String ID;

    public Holding(String dateCreated, double purchasePrice, int quantity, Instrument instrument) {
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
