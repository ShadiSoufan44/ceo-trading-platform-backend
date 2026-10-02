package com.ceo.trading_platform_backend.services;

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
    public Instrument getInstrumentById(int instrumentId) {
        return repository.getReferenceById(instrumentId);
    }

    @Transactional 
    public Instrument getInstrumentBySymbol(String symbol) {
        return repository.findBySymbol(symbol);
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
    public Instrument getOrInsertInstrumentBySymbol(String symbol) {
        Instrument instrument = getInstrumentBySymbol(symbol);
        if (instrument == null) {
            // Fetch real instrument metadata from market service
            var metadata = marketService.getSymbolMetadata(symbol);
            InstrumentType type = parseInstrumentType(metadata.type());
            instrument = insertInstrumentByFields(symbol, type, metadata.name());
        }
        return instrument;
    }
    
    private InstrumentType parseInstrumentType(String typeString) {
        if (typeString == null) {
            return InstrumentType.EQUITY; // default
        }
        try {
            return InstrumentType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return InstrumentType.EQUITY; // fallback default
        }
    }   
}