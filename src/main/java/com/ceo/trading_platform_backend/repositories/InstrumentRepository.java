package com.ceo.trading_platform_backend.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ceo.trading_platform_backend.models.Instrument;

public interface InstrumentRepository extends JpaRepository<Instrument, UUID> {
    
    public Instrument findBySymbol(String symbol);
}
