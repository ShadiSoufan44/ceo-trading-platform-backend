package com.ceo.trading_platform_backend.services;

import org.springframework.stereotype.Service;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.repositories.InstrumentRepository;

import jakarta.transaction.Transactional;

@Service  
public class InstrumentService {    
    private InstrumentRepository repository;
    // FIXME private MarketService marketService;
    
    public InstrumentService(
        InstrumentRepository instrumentRepository
        // , MarketService marketService;
    ) {
        this.repository = instrumentRepository;
        // this.marketService = marketService;
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
            String name; InstrumentType type;
            if (symbol == "USD") {
                // TODO: change to not be magic strings
                name = "United States Dollar";
                type = InstrumentType.CASH;
            } else {
                // TODO: Replace this with marketservice call!!
                name = "Test Instrument Name";
                type = InstrumentType.EQUITY;
            }
            instrument = insertInstrumentByFields(symbol, type, name);
        }
        return instrument;
    }    
}