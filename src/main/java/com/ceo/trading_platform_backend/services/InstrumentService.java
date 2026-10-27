package com.ceo.trading_platform_backend.services;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.models.InstrumentCandle;
import com.ceo.trading_platform_backend.repositories.InstrumentRepository;

import com.ceo.trading_platform_backend.dto.PerformanceResponse;

import jakarta.transaction.Transactional;

@Service  
public class InstrumentService {    
    private InstrumentRepository repository;
    private MarketService marketService;
    private InstrumentCacheService candleService;
    
    public InstrumentService(
        InstrumentRepository instrumentRepository,
        MarketService marketService,
        InstrumentCacheService candleService
    ) {
        this.repository = instrumentRepository;
        this.marketService = marketService;
        this.candleService = candleService;
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

    @Transactional
    public List<PerformanceResponse> getInstrumentPerformance(UUID instrumentId, String timeframe) {
        Instrument instrument = getInstrumentById(instrumentId);
        LocalDate end = LocalDate.now();
        LocalDate start = candleService.calculateStartDate(timeframe, end); 
        List<InstrumentCandle> candles = candleService.findOrInsertCandles(
            instrument.getSymbol(), instrument.getType(), start, end
        );
        NavigableMap<LocalDate, BigDecimal> closes = new TreeMap<>();
        for (InstrumentCandle candle : candles) {
            if (candle.getClose() != null) {
                closes.put(candle.getCandleDate().atZone(ZoneOffset.UTC).toLocalDate(), candle.getClose());
            }
        }
        List<PerformanceResponse> performanceResponses = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            BigDecimal balance = BigDecimal.ZERO;
            boolean complete = true;
            for (var entry : closes.entrySet()) {
                if (entry.getKey().isAfter(date)) {
                    break;
                }
                balance = entry.getValue(); // balance is straight up the price from candle, since not quantities
            }
            if (complete) {
                performanceResponses.add(new PerformanceResponse(
                    date.atStartOfDay(ZoneOffset.UTC).toInstant().toString(), balance
                ));
            }
        }
        return performanceResponses;
    }

}