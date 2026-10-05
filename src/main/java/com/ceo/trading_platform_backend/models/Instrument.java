package com.ceo.trading_platform_backend.models;

import java.util.UUID;

import com.ceo.trading_platform_backend.enums.InstrumentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table(name = "instrument")
public class Instrument {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "instrument_id")
    private UUID ID;

    @Column(name = "symbol")
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "instrument_type")
    private InstrumentType type;

    @Column(name = "full_name")
    private String fullName;

    public Instrument() {}

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
    public UUID getID() {
        return ID;
    }
}

