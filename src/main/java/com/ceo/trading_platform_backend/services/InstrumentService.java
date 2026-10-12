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
            String canonicalSymbol = metadata.symbol() != null && !metadata.symbol().isBlank()
                ? metadata.symbol()
                : symbol;
            instrument = insertInstrumentByFields(canonicalSymbol, type, metadata.name());
        }
        return instrument;
    }
    
    private InstrumentType parseInstrumentType(String typeString) {
        if (typeString == null || typeString.isBlank()) {
            return InstrumentType.EQUITY; // default
        }

        String normalizedType = typeString.trim().toUpperCase();
        return switch (normalizedType) {
            case "EQUITY", "STOCK" -> InstrumentType.EQUITY;
            case "CRYPTO" -> InstrumentType.CRYPTO;
            case "CASH", "FX", "FOREX" -> InstrumentType.CASH;
            default -> InstrumentType.EQUITY; // fallback default
        };
    }
}
