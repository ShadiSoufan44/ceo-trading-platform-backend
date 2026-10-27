package com.ceo.trading_platform_backend.services;
import java.math.BigDecimal;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.NavigableMap;

import com.ceo.trading_platform_backend.dto.PerformanceResponse;
import com.ceo.trading_platform_backend.models.InstrumentCandle;
import com.ceo.trading_platform_backend.models.Instrument;

@Service
public class InstrumentPerformanceService {
    private InstrumentService instrumentService;
    private InstrumentCacheService candleService;

    public InstrumentPerformanceService(
        InstrumentService instrumentService,
        InstrumentCacheService candleService
    ) {
        this.instrumentService = instrumentService;
        this.candleService = candleService;
    }
    
    @Transactional
    public List<PerformanceResponse> getInstrumentPerformance(UUID instrumentId, String timeframe) {
        Instrument instrument = instrumentService.getInstrumentById(instrumentId);
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
