package com.ceo.trading_platform_backend.uml_objects;

import com.ceo.trading_platform_backend.uml_objects.Enums.InstrumentType;

public class Instrument {
    int ID;
    String symbol;
    InstrumentType type;
    String fullName;


    public Instrument(String symbol, InstrumentType type, String fullName) {
        this.symbol = symbol;
        this.type = type;
        this.fullName = fullName;
    }

    public String getSymbol() {
        return symbol;
    }
    public InstrumentType getType() {
        return type;
    }
    public String getFullName() {
        return fullName;
    }
    public int getID() {
        return ID;
    }
}

