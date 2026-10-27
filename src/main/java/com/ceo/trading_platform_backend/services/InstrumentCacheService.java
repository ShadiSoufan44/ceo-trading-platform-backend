package com.ceo.trading_platform_backend.services;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
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
    private final Clock clock;

    private static final int MAX_MARKET_CLOSURE_DAYS = 7;

    @Autowired
    public InstrumentCacheService(
        InstrumentCandleRepository instrumentCandleRepository,
        MarketService marketService,
        InstrumentService instrumentService
    ) {
        this(instrumentCandleRepository, marketService, instrumentService, Clock.systemUTC());
    }

    // tests pass a fixed clock to control what "today" is
    InstrumentCacheService(
        InstrumentCandleRepository instrumentCandleRepository,
        MarketService marketService,
        InstrumentService instrumentService,
        Clock clock
    ) {
        this.instrumentCandleRepository = instrumentCandleRepository;
        this.marketService = marketService;
        this.instrumentService = instrumentService;
        this.clock = clock;
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

    // Daily candles for [from, to], served from the database when it is up to date.
    // Only finished days are returned and cached: today's candle is still moving, so it is never stored.
    // The cache is up to date when it reaches back to `from`, every candle in it is final, and the API
    // was already asked since the last finished day ended, so an instrument costs at most one API call a day.
    public List<InstrumentCandle> findOrInsertCandles(
            String instrumentSymbol,
            InstrumentType instrumentType,
            LocalDate from,
            LocalDate to
    ) {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        LocalDate lastFinishedDay = to.isBefore(today) ? to : today.minusDays(1);
        if (lastFinishedDay.isBefore(from)) {
            return List.of();
        }

        Instrument instrument = instrumentService.getInstrumentBySymbolAndType(instrumentSymbol, instrumentType);
        List<InstrumentCandle> cached = findCandles(instrument.getID(), from, lastFinishedDay);

        if (isUpToDate(cached, from, lastFinishedDay)) {
            System.out.println("Instrument cache full HIT | " + instrumentSymbol + ": " + instrumentType);
            return cached;
        }
        System.out.println("Instrument cache partial or full MISS | " + instrumentSymbol + ": " + instrumentType);

        Map<Instant, InstrumentCandle> cachedByDate = new HashMap<>();
        cached.forEach((candle) -> cachedByDate.putIfAbsent(candle.getCandleDate(), candle));

        List<InstrumentCandle> candles = new ArrayList<>();
        for (MarketService.Candle candle : marketService.getCandlesByType(instrumentSymbol, instrumentType, from, lastFinishedDay)) {
            if (candle.date().isAfter(lastFinishedDay)) {
                continue;
            }
            Instant candleDate = startOfDay(candle.date());
            // synthetic candles (the API filling a gap) come without a volume
            BigDecimal volume = candle.volume() == null ? null : new BigDecimal(candle.volume());
            boolean synthetic = Boolean.TRUE.equals(candle.synthetic());
            InstrumentCandle stored = cachedByDate.get(candleDate);
            if (stored == null) {
                stored = new InstrumentCandle(
                    instrument, candleDate,
                    candle.open(), candle.high(), candle.low(), candle.close(), candle.adjclose(),
                    volume, synthetic
                );
            }
            // also repairs candles that were cached before their day ended
            stored.refresh(
                candle.open(), candle.high(), candle.low(), candle.close(), candle.adjclose(),
                volume, synthetic, now
            );
            candles.add(stored);
        }

        return instrumentCandleRepository.saveAll(candles);
    }

    private boolean isUpToDate(List<InstrumentCandle> cached, LocalDate from, LocalDate lastFinishedDay) {
        if (cached.isEmpty()) {
            return false;
        }
        // markets are never closed for a week, so a fully cached range starts within a week of `from`
        LocalDate firstCachedDay = LocalDate.ofInstant(cached.get(0).getCandleDate(), ZoneOffset.UTC);
        if (firstCachedDay.isAfter(from.plusDays(MAX_MARKET_CLOSURE_DAYS))) {
            return false;
        }

        boolean allFinal = cached.stream().allMatch((candle) -> candle.getFetchedAt() != null
            && !candle.getFetchedAt().isBefore(candle.getCandleDate().plus(1, ChronoUnit.DAYS)));
        Instant lastFinishedDayEnded = startOfDay(lastFinishedDay.plusDays(1));
        boolean checkedSinceLastFinishedDay = cached.stream()
            .anyMatch((candle) -> candle.getFetchedAt() != null && !candle.getFetchedAt().isBefore(lastFinishedDayEnded));

        return allFinal && checkedSinceLastFinishedDay;
    }

    // ordered oldest first
    private List<InstrumentCandle> findCandles(UUID instrumentId, LocalDate from, LocalDate to) {
        return instrumentCandleRepository.findCandleInDateRange(
            instrumentId, startOfDay(from), startOfDay(to.plusDays(1))
        );
    }

    private Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
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

    // same as portfolio service except with one day
    public LocalDate calculateStartDate(String timeframe, LocalDate end) {
        if ("1D".equals(timeframe)) {
            return end.minusDays(1);
        } else if ("1W".equals(timeframe)) {
            return end.minusWeeks(1);
        } else if ("1M".equals(timeframe)) {
            return end.minusMonths(1);
        } else if ("3M".equals(timeframe)) {
            return end.minusMonths(3);
        } else if ("1Y".equals(timeframe)) {
            return end.minusYears(1);
        } else if ("5Y".equals(timeframe)) {
            return end.minusYears(5);
        } else {
            throw new IllegalArgumentException("Invalid timeframe: " + timeframe);
        }
    }

    public List<MarketService.Candle> getTodaysCandle(UUID instrumentId) {
        Instrument instrument = instrumentService.getInstrumentById(instrumentId);
        LocalDate today = LocalDate.now();
        List<MarketService.Candle> candles = marketService.getCandlesByType(instrument.getSymbol(), instrument.getType(), today, today);
        return candles;
    }
}
