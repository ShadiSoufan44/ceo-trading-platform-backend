package com.ceo.trading_platform_backend.services;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ceo.trading_platform_backend.repositories.OrderRepository;

/**
 * AnalystService handles analyst-specific reporting and market data operations.
 * For Priya (commercial analyst): Monthly reporting on trading volumes and client activity
 * without competing with live trading for database capacity.
 */
@Service
public class AnalystService {
    
    private final OrderRepository orderRepository;
    private final JdbcTemplate jdbcTemplate;

    public AnalystService(OrderRepository orderRepository, JdbcTemplate jdbcTemplate) {
        this.orderRepository = orderRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Get historical price data for a symbol from Finnhub API.
     * 
     * Returns: CandleData (OHLCV bars for charting)
     */
    @Transactional(readOnly = true)
    public Object getHistoricalData(String symbol) {
        return null;
    }

    /**
     * Generate trading volume report by instrument.
     * Groups orders by month and instrument symbol, returns total volumes
     * for Priya's monthly reporting to executive committee.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> generateVolumeReport() {
        String sql = """
            SELECT 
                DATE_TRUNC('month', o.place_date)::DATE as month_year,
                COUNT(*) as total_orders,
                COALESCE(SUM(CAST(h.quantity AS DECIMAL)), 0) as total_volume,
                i.symbol
            FROM "order" o
            JOIN "Holding" h ON o.holding_id = h.holding_id
            JOIN instrument i ON o.instr_id = i.instrument_id
            WHERE o.status = 'FULFILLED'
            GROUP BY DATE_TRUNC('month', o.place_date), i.symbol
            ORDER BY month_year DESC, i.symbol ASC
        """;
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
        return result != null ? result : new java.util.ArrayList<>();
    }

    /**
     * Generate trading activity report by client/time.
     * Groups orders by month and client, returns trade counts and volumes.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> generateActivityReport() {
        String sql = """
            SELECT 
                u.user_id,
                u.full_name,
                DATE_TRUNC('month', o.place_date)::DATE as month_year,
                COUNT(*) as total_trades,
                COALESCE(ROUND(SUM(CAST(h.quantity AS DECIMAL) * CAST(h.quote AS DECIMAL))::NUMERIC, 2), 0) as total_volume_value
            FROM "order" o
            JOIN "user_account" u ON o.user_id = u.user_id
            JOIN "Holding" h ON o.holding_id = h.holding_id
            WHERE o.status = 'FULFILLED' AND u.role = 'CLIENT'
            GROUP BY u.user_id, u.full_name, DATE_TRUNC('month', o.place_date)
            ORDER BY month_year DESC, total_volume_value DESC
        """;
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
        return result != null ? result : new java.util.ArrayList<>();
    }
}
