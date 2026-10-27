package com.ceo.trading_platform_backend.services;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.repositories.InstrumentRepository;

import jakarta.transaction.Transactional;

@Service  
public class InstrumentService {    
    private InstrumentRepository repository;
    private MarketService marketService;
    
    public InstrumentService(
        InstrumentRepository instrumentRepository,
        MarketService marketService
    ) {
        this.repository = instrumentRepository;
        this.marketService = marketService;
    }

    @Transactional 
    public Instrument getInstrumentById(UUID instrumentId) {
        return repository.getReferenceById(instrumentId);
    }

    @Transactional 
    public Instrument getInstrumentBySymbolAndType(String symbol, InstrumentType type) {
        return repository.findBySymbolAndType(symbol, type);
    }
    
    @Transactional 
    public Instrument insertInstrumentByFields(
        String symbol, 
        InstrumentType type, 
        String name
    ) {
        Instrument instrument = new Instrument(symbol, type, name);
        repository.save(instrument);
        return instrument;
    }

    @Transactional 
    public Instrument getOrInsertInstrumentBySymbolAndType(String symbol, InstrumentType type) {
        Instrument instrument = getInstrumentBySymbolAndType(symbol, type);
        if (instrument == null) {
            // Fetch real instrument metadata from market service
            var metadata = marketService.getSymbolMetadataByType(symbol, type);
            instrument = insertInstrumentByFields(symbol, type, metadata.name());
        }
        return instrument;
    }


}