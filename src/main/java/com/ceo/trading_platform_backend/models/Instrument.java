package com.ceo.trading_platform_backend.models;

import com.ceo.trading_platform_backend.enums.InstrumentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table(name = "instrument")
public class Instrument {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "instrument_id")
    int ID;

    @Column(name = "symbol")
    String symbol;

    @Column(name = "instrument_type")
    InstrumentType type;

    @Column(name = "full_name")
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

