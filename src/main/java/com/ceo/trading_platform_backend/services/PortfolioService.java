package com.ceo.trading_platform_backend.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.enums.Side;
import com.ceo.trading_platform_backend.exception.ResourceNotFoundException;
import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.models.InstrumentCandle;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.Portfolio;
import com.ceo.trading_platform_backend.dto.HoldingSummaryResponse;
import com.ceo.trading_platform_backend.dto.PerformanceResponse;
import com.ceo.trading_platform_backend.repositories.PortfolioRepository;
import com.ceo.trading_platform_backend.services.MarketService.QuoteData;
import com.ceo.trading_platform_backend.utils.MoneyUtilities;

import jakarta.transaction.Transactional;

@Service 
public class PortfolioService {
    private final InstrumentCacheService instrumentCacheService;
    private final PortfolioRepository repository;
    private final InstrumentService instrumentService;
    private final MarketService marketService;

    private final String US_CURRENCY_SYMBOL = "USD";

    public PortfolioService(
        PortfolioRepository repository,
        InstrumentService instrumentService, 
        InstrumentCacheService instrumentCacheService,
        MarketService marketService
    ) {
        this.repository = repository;
        this.instrumentService = instrumentService;
        this.instrumentCacheService = instrumentCacheService;
        this.marketService = marketService;
    }

    private BigDecimal getPortfolioBuyingPower(Portfolio portfolio) {
        BigDecimal totalValue = new BigDecimal(0);
        for (Holding holding : portfolio.getHoldings()) {
            if (holding.getInstrument().getSymbol().equals(US_CURRENCY_SYMBOL)) {
                totalValue = totalValue.add(holding.getQuantity());
            }
        }
        return totalValue;
    }

    private BigDecimal getPortfolioTotalValue(Portfolio portfolio) {
        BigDecimal value = new BigDecimal(0);

        for (Holding holding : portfolio.getHoldings()) {
            BigDecimal price;
            if (holding.getInstrument().getSymbol().equals(US_CURRENCY_SYMBOL)) {
                price = new BigDecimal(1);
            } else {
                Instrument instrument = holding.getInstrument();
                price = marketService.getPriceWithType(
                    instrument.getType(), instrument.getSymbol()
                );
            }
            value = value.add(price.multiply(holding.getQuantity()));
        }
        return value;
    }

    public Portfolio getPortfolioById(UUID portfolioId) {
        try {
            Portfolio portfolio = repository.getReferenceById(portfolioId);
            return portfolio;
        } catch (Exception e) {
            throw new ResourceNotFoundException("Portfolio not found with id " + portfolioId);
        }
    }
    
    public BigDecimal getBuyingPowerByPortfolioId(UUID portfolioId) {
        Portfolio portfolio = getPortfolioById(portfolioId);
        return getPortfolioBuyingPower(portfolio);
    }

    public BigDecimal getTotalValueByPortfolioId(UUID portfolioId) {
        Portfolio portfolio = getPortfolioById(portfolioId);
        return getPortfolioTotalValue(portfolio);
    }

    public List<Holding> getHoldingsByPortfolioId(UUID portfolioId) {
        Portfolio portfolio = getPortfolioById(portfolioId);
        if (portfolio == null) return null;
        return portfolio.getHoldings();
    }

    @Transactional
    public List<HoldingSummaryResponse> getHoldingSummariesByPortfolioId(UUID portfolioId) {

        Portfolio portfolio = getPortfolioById(portfolioId);
        List<Holding> holdings = portfolio.getHoldings();

        int counter = 1;
        List<List<Holding>> chunkedHoldings = new ArrayList<>();
        List<Holding> chunk = new ArrayList<>();

        for (Holding holding : holdings) {
            if (counter >= 25) {
                counter = 1;
                chunkedHoldings.add(chunk);
                chunk = new ArrayList<>();
            }
            chunk.add(holding);
            counter++;   
        }
        if (counter != 0) {
            chunkedHoldings.add(chunk);
        }

        List<QuoteData> fullQuotesList = new ArrayList<>();

        for (List<Holding> holdingChunk : chunkedHoldings) {
            List<String> symbols = holdingChunk
                    .stream()
                    .map((Holding holding) -> marketService.normalizeSymbolByType(
                        holding.getInstrument().getSymbol(), holding.getInstrument().getType()
                    ))
                    .toList();
            
            List<QuoteData> quotes = marketService.getQuotes(symbols)
                    .stream().map((val) -> val.quote())
                    .toList();
            fullQuotesList.addAll(quotes);
        }

        List<HoldingSummaryResponse> summaries = new ArrayList<>();

        for (int i = 0; i < fullQuotesList.size(); i++) {
            HoldingSummaryResponse summary = toHoldingSummaryResponse(
                portfolioId,
                holdings.get(i),
                fullQuotesList.get(i)
            );
            summaries.add(summary);
        }

        return summaries;
    }

    @Transactional
    public List<PerformanceResponse> getPortfolioPerformance(UUID portfolioId, String timeframe) {
        LocalDate end = LocalDate.now();
        LocalDate start = calculateStartDate(timeframe, end);
        Portfolio portfolio = getPortfolioById(portfolioId);
        if (portfolio.getHoldings().isEmpty()) {
            return List.of();
        }

        // Preserve current-position valuation while consolidating repeated holding rows.
        Map<PerformanceInstrument, BigDecimal> quantities = new LinkedHashMap<>();
        for (Holding holding : portfolio.getHoldings()) {
            Instrument instrument = holding.getInstrument();
            PerformanceInstrument key = new PerformanceInstrument(instrument.getSymbol(), instrument.getType());
            quantities.merge(key, holding.getQuantity(), BigDecimal::add);
        }

        Map<PerformanceInstrument, NavigableMap<LocalDate, BigDecimal>> prices = new LinkedHashMap<>();
        for (var position : quantities.entrySet()) {
            PerformanceInstrument instrument = position.getKey();
            if (position.getValue().signum() == 0 || isPerformanceUsd(instrument)) {
                continue;
            }
            NavigableMap<LocalDate, BigDecimal> closes = new TreeMap<>();
            List<InstrumentCandle> candles = instrumentCacheService.findOrInsertCandles(
                instrument.symbol(), instrument.type(), start, end
            );
            for (InstrumentCandle candle : candles) {
                if (candle.getClose() != null) {
                    closes.put(candle.getCandleDate().atZone(ZoneOffset.UTC).toLocalDate(), candle.getClose());
                }
            }
            prices.put(instrument, closes);
        }

        List<PerformanceResponse> performanceResponses = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            BigDecimal balance = BigDecimal.ZERO;
            boolean complete = true;
            for (var position : quantities.entrySet()) {
                if (position.getValue().signum() == 0) {
                    continue;
                }
                PerformanceInstrument instrument = position.getKey();
                BigDecimal price = BigDecimal.ONE;
                if (!isPerformanceUsd(instrument)) {
                    // Use the latest known close, never a future price, on non-trading days.
                    var close = prices.get(instrument).floorEntry(date);
                    if (close == null) {
                        complete = false;
                        break;
                    }
                    price = close.getValue();
                }
                balance = balance.add(price.multiply(position.getValue()));
            }
            // A range may begin before its first trading day. Do not invent missing prices.
            if (complete) {
                performanceResponses.add(new PerformanceResponse(
                    date.atStartOfDay(ZoneOffset.UTC).toInstant().toString(), balance
                ));
            }
        }
        return performanceResponses;
    }

    private record PerformanceInstrument(String symbol, InstrumentType type) {
    }

    private boolean isPerformanceUsd(PerformanceInstrument instrument) {
        return instrument.type() == InstrumentType.CASH && US_CURRENCY_SYMBOL.equals(instrument.symbol());
    }

    private LocalDate calculateStartDate(String timeframe, LocalDate end) {
        if ("1W".equals(timeframe)) {
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

    @Transactional 
    private void addDeposit(Portfolio portfolio, Order order, BigDecimal amount, String currency, InstrumentType type) {
        Holding depositHolding = new Holding(
            Instant.now(),
            BigDecimal.ONE,
            amount,
            instrumentService.getInstrumentBySymbolAndType(currency, type),
            order,
            portfolio
        );
        portfolio = repository.save(portfolio);
        portfolio.addHolding(depositHolding);
        repository.save(portfolio);
    }
    
    @Transactional 
    public void updateHoldingsFromOrder(
            Order order, BigDecimal addPrice, BigDecimal subPrice
    ) {
        Portfolio portfolio = getPortfolioById(order.getPortfolioId());
        Side side = order.getSide();
        Instrument addInstrument = instrumentService.getInstrumentById(
            order.getAddInstrumentId()
        );
        Instrument subInstrument = null;
        if (side != Side.DEPOSIT) {
            subInstrument = instrumentService.getInstrumentById(
                order.getSubtractInstrumentId()
            );
        }
        
        Instant now = Instant.now();
        // addQuantity * addPrice = subQuantity * subPrice
        BigDecimal addQuantity;
        BigDecimal subQuantity;
        if (side == Side.BUY) {
            addQuantity = order.getQuantity();
            subQuantity = MoneyUtilities.safeDivide(
                addQuantity.multiply(addPrice), 
                subPrice
            );
        } else if (side == Side.SELL) {
            subQuantity = order.getQuantity();
            addQuantity = MoneyUtilities.safeDivide(
                subQuantity.multiply(subPrice), 
                addPrice
            );
        } else /* if (side == Side.DEPOSIT) */ {
            addQuantity = order.getQuantity();
            subQuantity = BigDecimal.ZERO;
        }

        portfolio = repository.save(portfolio);

        Holding addHolding = new Holding(
            now, addPrice, addQuantity,
            addInstrument, order, portfolio
        );
        portfolio.addHolding(addHolding);

        if (side != Side.DEPOSIT) {
            Holding subHolding = new Holding(
                now, subPrice, subQuantity.negate(),
                subInstrument, order, portfolio
            );
            portfolio.addHolding(subHolding);
        }
        

        repository.save(portfolio);
    }


    private HoldingSummaryResponse toHoldingSummaryResponse(UUID portfolioId, Holding holding, QuoteData quote) {
        Instrument instrument = holding.getInstrument();
        String symbol = instrument.getSymbol();
        InstrumentType type = instrument.getType();
        List<InstrumentCandle> candles = instrumentCacheService.findOrInsertCandles(
            symbol, type, LocalDate.now().minusYears(1), LocalDate.now()
        );

        BigDecimal weekLow52 = candles.stream()
            .map(InstrumentCandle::getLow)
            .min(Comparator.naturalOrder())
            .orElse(quote.price());
        BigDecimal weekHigh52 = candles.stream()
            .map(InstrumentCandle::getHigh)
            .max(Comparator.naturalOrder())
            .orElse(quote.price());

        Map<String, BigDecimal> percentChange = new LinkedHashMap<>();
        percentChange.put("1D", quote.changePercent());
        percentChange.put("5D", computePercentChange(candles, 5));
        percentChange.put("1M", computePercentChange(candles, 30));
        percentChange.put("6M", computePercentChange(candles, 182));
        percentChange.put("1Y", computePercentChange(candles, 365));

        return new HoldingSummaryResponse(
            holding.getID(),
            portfolioId,
            symbol,
            instrument.getFullName(),
            instrument.getType(),
            holding.getQuantity(),
            holding.getPurchasedPrice(),
            quote.price(),
            quote.change(),
            percentChange,
            weekLow52,
            weekHigh52
        );
    }

    private BigDecimal computePercentChange(List<InstrumentCandle> candles, int daysBack) {
        if (candles == null || candles.isEmpty()) {
            return BigDecimal.ZERO;
        }

        LocalDate cutoff = LocalDate.now().minusDays(daysBack);
        List<InstrumentCandle> window = candles.stream()
            .filter((candle) -> !candle.getCandleDate().atZone(ZoneOffset.UTC).toLocalDate().isBefore(cutoff))
            .sorted(Comparator.comparing(InstrumentCandle::getCandleDate))
            .toList();

        if (window.isEmpty()) {
            window = candles.stream()
                .sorted(Comparator.comparing(InstrumentCandle::getCandleDate))
                .toList();
        }

        if (window.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal start = window.get(0).getClose();
        BigDecimal end = window.get(window.size() - 1).getClose();
        if (start == null || start.compareTo(BigDecimal.ZERO) == 0 || end == null) {
            return BigDecimal.ZERO;
        }

        return MoneyUtilities.safeDivide(end.subtract(start), start);
    }
}