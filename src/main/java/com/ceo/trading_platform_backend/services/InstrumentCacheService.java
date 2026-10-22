package com.ceo.trading_platform_backend.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.models.InstrumentCandle;
import com.ceo.trading_platform_backend.repositories.InstrumentCandleRepository;

@Service 
public class InstrumentCacheService {
    
    private final InstrumentCandleRepository instrumentCandleRepository;
    private final MarketService marketService;
    private final InstrumentService instrumentService;

    public InstrumentCacheService(
        InstrumentCandleRepository instrumentCandleRepository,
        MarketService marketService,
        InstrumentService instrumentService
    ) {
        this.instrumentCandleRepository = instrumentCandleRepository;
        this.marketService = marketService;
        this.instrumentService = instrumentService;
    }
    
    private Optional<InstrumentCandle> findCandleByDate(UUID instrumentId, LocalDate date) {

        List<InstrumentCandle> candles = instrumentCandleRepository.findByInstrumentIDAndCandleDate(
            instrumentId, date.atStartOfDay(ZoneOffset.UTC).toInstant()
        );
        if (candles.size() == 0) {
            return Optional.empty();
        } else if (candles.size() == 1) {
            return Optional.of(candles.getFirst());
        } else {
            System.out.println("!!! There shouldn't be multiple candles for the same date for the same instrument !!!");
            return Optional.of(candles.getFirst());
        }
    }

    public List<InstrumentCandle> findOrInsertCandles(
            String instrumentSymbol,
            InstrumentType instrumentType,
            LocalDate from,
            LocalDate to
    ) {
        Instrument instrument = instrumentService.getInstrumentBySymbolAndType(instrumentSymbol, instrumentType);
        List<InstrumentCandle> candles = findCandles(instrumentSymbol, instrumentType, from, to);

        long askedNumberOfDays = ChronoUnit.DAYS.between(from, to);

        if (candles.size() < askedNumberOfDays) {
            System.out.println("Instrument cache partial or full MISS");
            List<MarketService.Candle> newCandles = marketService.getCandlesByType(instrumentSymbol, instrumentType, from, to);

            candles = new ArrayList<InstrumentCandle>();
            for (MarketService.Candle candle : newCandles) {
                Optional<InstrumentCandle> existingCandle = findCandleByDate(instrument.getID(), candle.date());
                if (existingCandle.isEmpty()) {
                    InstrumentCandle newCandle = new InstrumentCandle(
                        instrument, candle.date().atStartOfDay(ZoneOffset.UTC).toInstant(), 
                        candle.open(), candle.high(), candle.low(), candle.close(), candle.adjclose(), 
                        new BigDecimal(candle.volume()), 
                        candle.synthetic().booleanValue()
                    );
                    newCandle = instrumentCandleRepository.save(newCandle);
                    candles.add(newCandle);
                } else {
                    candles.add(existingCandle.get());
                }
            }
        } else {
            System.out.println("Instrument cache full HIT");
        }

        return candles;
    }

    private List<InstrumentCandle> findCandles(
            String instrumentSymbol,
            InstrumentType instrumentType,
            LocalDate from,
            LocalDate to
    ) {
        Instant fromInclusive = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toExclusive = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        UUID instrumentId = instrumentService.getInstrumentBySymbolAndType(instrumentSymbol, instrumentType).getID();

        return instrumentCandleRepository.findCandleInDateRange(
            instrumentId, fromInclusive, toExclusive
        );
    }

    public BigDecimal getPriceWithTypeAtDate(
                String instrumentSymbol, 
                InstrumentType instrumentType,
                LocalDate date
    ) {
        Instrument instrument = instrumentService.getOrInsertInstrumentBySymbolAndType(
            instrumentSymbol, instrumentType
        );

        InstrumentCandle candle = findCandleByDate(instrument.getID(), date).orElse(null);
        return candle.getClose();
    }
}
