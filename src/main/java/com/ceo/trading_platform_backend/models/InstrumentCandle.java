package com.ceo.trading_platform_backend.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "instrument_candle")
public class InstrumentCandle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "instrument_candle_id")
    private UUID instrumentCandleId;

    @ManyToOne
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "candle_date")
    private Instant candleDate;

    @Column(name = "open")
    private BigDecimal open;

    @Column(name = "high")
    private BigDecimal high;

    @Column(name = "low")
    private BigDecimal low;

    @Column(name = "close")
    private BigDecimal close;

    @Column(name = "adj_close")
    private BigDecimal adjustedClose;

    @Column(name = "volume")
    private BigDecimal volume;

    @Column(name = "synthetic")
    private boolean synthetic;

    // null for candles cached before this was tracked
    @Column(name = "fetched_at")
    private Instant fetchedAt;

    public InstrumentCandle() {
    }

    public InstrumentCandle(
        Instrument instrument,
        Instant candleDate,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal adjustedClose,
        BigDecimal volume,
        boolean synthetic
    ) {
        this.instrument = instrument;
        this.candleDate = candleDate;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.adjustedClose = adjustedClose;
        this.volume = volume;
        this.synthetic = synthetic;
    }

    public UUID getInstrumentCandleId() {
        return instrumentCandleId;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public Instant getCandleDate() {
        return candleDate;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public BigDecimal getClose() {
        return close;
    }

    public BigDecimal getAdjustedClose() {
        return adjustedClose;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public boolean getSynthetic() {
        return synthetic;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(Instant fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    // overwrite with freshly fetched values, e.g. when the cached candle was saved before its day ended
    public void refresh(
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal adjustedClose,
        BigDecimal volume,
        boolean synthetic,
        Instant fetchedAt
    ) {
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.adjustedClose = adjustedClose;
        this.volume = volume;
        this.synthetic = synthetic;
        this.fetchedAt = fetchedAt;
    }
}
