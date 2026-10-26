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
import com.ceo.trading_platform_backend.enums.PortfolioType;
import com.ceo.trading_platform_backend.enums.Side;
import com.ceo.trading_platform_backend.exception.ResourceNotFoundException;
import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.models.InstrumentCandle;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.Portfolio;
import com.ceo.trading_platform_backend.models.User;
import com.ceo.trading_platform_backend.dto.HoldingSummaryResponse;
import com.ceo.trading_platform_backend.dto.PerformanceResponse;
import com.ceo.trading_platform_backend.repositories.PortfolioRepository;
import com.ceo.trading_platform_backend.services.MarketService.QuoteData;
import com.ceo.trading_platform_backend.utils.MoneyUtilities;
import com.ceo.trading_platform_backend.services.UserService;


import jakarta.transaction.Transactional;

@Service 
public class PortfolioService {
    private final InstrumentCacheService instrumentCacheService;
    private final PortfolioRepository repository;
    private final InstrumentService instrumentService;
    private final MarketService marketService;
    private final UserService userService;

    private final String US_CURRENCY_SYMBOL = "USD";
    private static final int CANDLE_LOOKBACK_BUFFER_DAYS = 7;

    public PortfolioService(
        PortfolioRepository repository,
        InstrumentService instrumentService, 
        InstrumentCacheService instrumentCacheService,
        MarketService marketService,
        UserService userService
    ) {
        this.repository = repository;
        this.instrumentService = instrumentService;
        this.instrumentCacheService = instrumentCacheService;
        this.marketService = marketService;
        this.userService = userService;
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

    @Transactional 
    private BigDecimal getPortfolioTotalValue(Portfolio portfolio) {
        List<QuoteData> quotes = getQuotesOfOwnedInstruments(portfolio);
        List<Holding> holdings = portfolio.getHoldings();
        BigDecimal totalValue = new BigDecimal(0);
        for (int i = 0; i < quotes.size(); i++) {
            System.out.println("Price: " + quotes.get(i).price() + ", Quantity:" + holdings.get(i).getQuantity());
            BigDecimal value = quotes.get(i).price().multiply(holdings.get(i).getQuantity());
            totalValue = totalValue.add(value);
        }
        return totalValue;
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

    @Transactional 
    public BigDecimal getTotalValueByPortfolioId(UUID portfolioId) {
        Portfolio portfolio = getPortfolioById(portfolioId);
        return getPortfolioTotalValue(portfolio);
    }

    public List<Holding> getHoldingsByPortfolioId(UUID portfolioId) {
        Portfolio portfolio = getPortfolioById(portfolioId);
        if (portfolio == null) return null;
        return portfolio.getHoldings();
    }

    private List<QuoteData> getQuotesOfOwnedInstruments(Portfolio portfolio) {
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
        return fullQuotesList;
    }

    @Transactional
    public List<HoldingSummaryResponse> getHoldingSummariesByPortfolioId(UUID portfolioId) {
        Portfolio portfolio = getPortfolioById(portfolioId);
        List<Holding> holdings = portfolio.getHoldings();
        List<QuoteData> fullQuotesList = getQuotesOfOwnedInstruments(portfolio);

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
        LocalDate start = calculateStartDate(timeframe, end); // all instrument candles share the start date, calculate once
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

    // method to create portfolio o`bject and save to db
    @Transactional
    public Portfolio createPortfolio(PortfolioType type, String clientId) {
        Portfolio portfolio = new Portfolio(type);
        User client = userService.getUserEntity(UUID.fromString(clientId));
        portfolio.setClient(client);
        return repository.save(portfolio);
    }

    private HoldingSummaryResponse toHoldingSummaryResponse(UUID portfolioId, Holding holding, QuoteData quote) {
        Instrument instrument = holding.getInstrument();
        String symbol = instrument.getSymbol();
        InstrumentType type = instrument.getType();
        // a few extra days so the 1Y change still has a close from on or before a year ago
        List<InstrumentCandle> candles = instrumentCacheService.findOrInsertCandles(
            symbol, type, LocalDate.now().minusYears(1).minusDays(CANDLE_LOOKBACK_BUFFER_DAYS), LocalDate.now()
        );

        // the session the live quote belongs to; its candle (if any) is partial, so changes start from earlier closes
        LocalDate sessionDate = quote.asOf() != null
            ? quote.asOf().atZone(ZoneOffset.UTC).toLocalDate()
            : LocalDate.now(ZoneOffset.UTC);
        LocalDate yearAgo = sessionDate.minusYears(1);

        BigDecimal weekLow52 = candles.stream()
            .filter((candle) -> !candleDate(candle).isBefore(yearAgo))
            .map(InstrumentCandle::getLow)
            .min(Comparator.naturalOrder())
            .orElse(quote.price());
        BigDecimal weekHigh52 = candles.stream()
            .filter((candle) -> !candleDate(candle).isBefore(yearAgo))
            .map(InstrumentCandle::getHigh)
            .max(Comparator.naturalOrder())
            .orElse(quote.price());

        List<InstrumentCandle> priorCloses = candles.stream()
            .filter((candle) -> candle.getClose() != null && candleDate(candle).isBefore(sessionDate))
            .sorted(Comparator.comparing(InstrumentCandle::getCandleDate))
            .toList();

        // all fractions (0.0123 = 1.23%); null when there isn't enough history for the period.
        // 1D is worked out here because the API's changePercent is already in percent
        Map<String, BigDecimal> percentChange = new LinkedHashMap<>();
        percentChange.put("1D", computePercentChange(quote.previousClose(), quote.price()));
        percentChange.put("5D", computePercentChange(closeSessionsBack(priorCloses, 5), quote.price()));
        percentChange.put("1M", computePercentChange(closeOnOrBefore(priorCloses, sessionDate.minusMonths(1)), quote.price()));
        percentChange.put("6M", computePercentChange(closeOnOrBefore(priorCloses, sessionDate.minusMonths(6)), quote.price()));
        percentChange.put("1Y", computePercentChange(closeOnOrBefore(priorCloses, yearAgo), quote.price()));

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

    private BigDecimal computePercentChange(BigDecimal start, BigDecimal end) {
        if (start == null || start.compareTo(BigDecimal.ZERO) == 0 || end == null) {
            return null;
        }
        return MoneyUtilities.safeDivide(end.subtract(start), start);
    }

    // close of the session `sessions` trading days before the current one; priorCloses is sorted oldest first
    private BigDecimal closeSessionsBack(List<InstrumentCandle> priorCloses, int sessions) {
        if (priorCloses.size() < sessions) {
            return null;
        }
        return priorCloses.get(priorCloses.size() - sessions).getClose();
    }

    // last close on or before the date, i.e. the price the period started from
    private BigDecimal closeOnOrBefore(List<InstrumentCandle> priorCloses, LocalDate date) {
        BigDecimal close = null;
        for (InstrumentCandle candle : priorCloses) {
            if (candleDate(candle).isAfter(date)) {
                break;
            }
            close = candle.getClose();
        }
        return close;
    }

    private LocalDate candleDate(InstrumentCandle candle) {
        return candle.getCandleDate().atZone(ZoneOffset.UTC).toLocalDate();
    }
}