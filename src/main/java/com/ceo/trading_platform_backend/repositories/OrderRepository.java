package com.ceo.trading_platform_backend.repositories;

import com.ceo.trading_platform_backend.uml_objects.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * OrderRepository for querying orders across the system.
 * Used by AdminService (trade lookups) and AnalystService (volume/activity reports).
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    /**
     * Find all orders for a specific user (client).
     * Used by ClientService to get transaction history.
     */
    List<Order> findByUserIdOrderByPlaceDateDesc(Integer userId);

    /**
     * Find orders within a date range for a specific user.
     * Used by AdminService for filtered trade searches.
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId " +
           "AND o.placeDate BETWEEN :startDate AND :endDate " +
           "ORDER BY o.placeDate DESC")
    List<Order> findByUserIdAndDateRange(
            @Param("userId") Integer userId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    /**
     * Find orders by symbol within a date range.
     * Used by AdminService for trade searches by instrument.
     */
    @Query("SELECT o FROM Order o WHERE o.instrument.symbol = :symbol " +
           "AND o.placeDate BETWEEN :startDate AND :endDate " +
           "ORDER BY o.placeDate DESC")
    List<Order> findBySymbolAndDateRange(
            @Param("symbol") String symbol,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    /**
     * Get all orders ordered by date (for paginated queries).
     * Used by AdminService for filtered trade searches with pagination.
     */
    @Query("SELECT o FROM Order o ORDER BY o.placeDate DESC")
    List<Order> findAllOrderByPlaceDate();
}
