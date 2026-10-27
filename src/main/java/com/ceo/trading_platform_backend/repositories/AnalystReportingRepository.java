package com.ceo.trading_platform_backend.repositories;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ceo.trading_platform_backend.models.ReportingTradeFact;

public interface AnalystReportingRepository extends JpaRepository<ReportingTradeFact, UUID> {

    @Query(value = """
            SELECT
              COALESCE(SUM(notional), 0) AS total_notional,
              COALESCE(SUM(quantity), 0) AS total_quantity,
              COUNT(*) AS executed_trades
            FROM reporting_trade_fact
            WHERE executed_at >= :fromTs
              AND executed_at < :toTs
              AND side <> 'DEPOSIT'
              AND (:symbol IS NULL OR symbol = :symbol)
              AND (:clientSegment IS NULL OR client_segment = :clientSegment)
            """, nativeQuery = true)
    List<Object[]> getTradingVolume(
            @Param("fromTs") Instant from,
            @Param("toTs") Instant to,
            @Param("symbol") String symbol,
            @Param("clientSegment") String clientSegment);

    @Query(value = """
            SELECT
              symbol,
              COALESCE(SUM(notional), 0) AS total_notional,
              COALESCE(SUM(quantity), 0) AS total_quantity,
              COUNT(*) AS executed_trades
            FROM reporting_trade_fact
            WHERE executed_at >= :fromTs
              AND executed_at < :toTs
              AND side <> 'DEPOSIT'
            GROUP BY symbol
            ORDER BY total_notional DESC
            LIMIT :resultLimit
            """, nativeQuery = true)
    List<Object[]> getTopInstruments(
            @Param("fromTs") Instant from,
            @Param("toTs") Instant to,
            @Param("resultLimit") int limit);

    @Query(value = """
            SELECT
              date_trunc('day', executed_at) AS period_start,
              client_segment,
              COALESCE(SUM(notional), 0) AS total_notional,
              COALESCE(SUM(quantity), 0) AS total_quantity,
              COUNT(*) AS executed_trades
            FROM reporting_trade_fact
            WHERE executed_at >= :fromTs
              AND executed_at < :toTs
              AND side <> 'DEPOSIT'
              AND (:clientSegment IS NULL OR client_segment = :clientSegment)
            GROUP BY period_start, client_segment
            ORDER BY period_start ASC, client_segment ASC
            """, nativeQuery = true)
    List<Object[]> getClientActivityTrend(
            @Param("fromTs") Instant from,
            @Param("toTs") Instant to,
            @Param("clientSegment") String clientSegment);
}
