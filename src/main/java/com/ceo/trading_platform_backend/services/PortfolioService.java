package com.ceo.trading_platform_backend.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ceo.trading_platform_backend.enums.Side;
import com.ceo.trading_platform_backend.exception.ResourceNotFoundException;
import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.Portfolio;
import com.ceo.trading_platform_backend.dto.HoldingSummaryResponse;
import com.ceo.trading_platform_backend.repositories.PortfolioRepository;
import com.ceo.trading_platform_backend.utils.MoneyUtilities;

import jakarta.transaction.Transactional;

@Service 
public class PortfolioService {
    private final PortfolioRepository repository;
    private final InstrumentService instrumentService;
    private final MarketService marketService;

    private final String US_CURRENCY_SYMBOL = "USD";

    public PortfolioService(
        PortfolioRepository repository,
        InstrumentService instrumentService,
        MarketService marketService
    ) {
        this.repository = repository;
        this.instrumentService = instrumentService;
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
        return portfolio.getHoldings().stream()
            .map((holding) -> toHoldingSummaryResponse(portfolioId, holding))
            .toList();
    }

    @Transactional 
    private void addDeposit(Portfolio portfolio, Order order, BigDecimal amount, String currency) {
        Holding depositHolding = new Holding(
            Instant.now(),
            BigDecimal.ONE,
            amount,
            instrumentService.getInstrumentBySymbol(currency),
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

    private HoldingSummaryResponse toHoldingSummaryResponse(UUID portfolioId, Holding holding) {
        Instrument instrument = holding.getInstrument();
        String symbol = instrument.getSymbol();
        MarketService.QuoteData quote = marketService.getQuote(symbol);
        List<MarketService.Candle> candles = marketService.getCandles(symbol, LocalDate.now().minusYears(1), LocalDate.now());

        BigDecimal weekLow52 = candles.stream()
            .map(MarketService.Candle::low)
            .min(Comparator.naturalOrder())
            .orElse(quote.price());
        BigDecimal weekHigh52 = candles.stream()
            .map(MarketService.Candle::high)
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
            holding.getQuantity(),
            holding.getPurchasedPrice(),
            quote.price(),
            quote.change(),
            percentChange,
            weekLow52,
            weekHigh52
        );
    }

    private BigDecimal computePercentChange(List<MarketService.Candle> candles, int daysBack) {
        if (candles == null || candles.isEmpty()) {
            return BigDecimal.ZERO;
        }

        LocalDate cutoff = LocalDate.now().minusDays(daysBack);
        List<MarketService.Candle> window = candles.stream()
            .filter((candle) -> !candle.date().isBefore(cutoff))
            .sorted(Comparator.comparing(MarketService.Candle::date))
            .toList();

        if (window.isEmpty()) {
            window = candles.stream()
                .sorted(Comparator.comparing(MarketService.Candle::date))
                .toList();
        }

        if (window.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal start = window.get(0).close();
        BigDecimal end = window.get(window.size() - 1).close();
        if (start == null || start.compareTo(BigDecimal.ZERO) == 0 || end == null) {
            return BigDecimal.ZERO;
        }

        return MoneyUtilities.safeDivide(end.subtract(start), start);
    }
}

// BUY US Equity: negative USD, positive US equity
// SELL US Equity: positive USD, negative US equity
    // 9:00 to 4:30 NY Time
    // Can only use USD
// BUY Indian Equity: negative INR, positive Indian equity
// SELL Indian Equity: positive INR, negative Indian equity
    // 9:15 to 3:30 IST
    // Can only use USD or INR
// DEPOSIT: positive money
    // Any time
    // USD or INR maybe (???)
// BUY Crypto: negative money, positive crypto
// SELL Crypto: positive money, negative crypto
    // Any time
    // Can use any currency to buy and sell crypto (???)
// BUY Forex: negative USD, positive other currency
// SELL Forex: positive USD, negative other currency
    // Any time
    // Can trade any currency for any other