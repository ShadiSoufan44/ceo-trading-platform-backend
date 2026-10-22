package com.ceo.trading_platform_backend.repositories;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ceo.trading_platform_backend.models.InstrumentCandle;

public interface InstrumentCandleRepository 
        extends JpaRepository<InstrumentCandle, UUID> {

    @Query(value = """
            SELECT *
            FROM instrument_candle
            WHERE instrument_id = :instrumentId
              AND candle_date >= :fromDate
              AND candle_date < :toDate
            ORDER BY candle_date ASC
            """, nativeQuery = true)
    List<InstrumentCandle> findCandleInDateRange(
            @Param("instrumentId") UUID instrumentId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );

    List<InstrumentCandle> findByInstrumentIDAndCandleDate(UUID instrumentId, Instant candleDate);
}