package com.ceo.trading_platform_backend.repositories;

import com.ceo.trading_platform_backend.uml_objects.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * PortfolioRepository for querying portfolios.
 * Used by ClientService to retrieve client portfolios.
 */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Integer> {

    /**
     * Find all portfolios for a specific client (user).
     */
    List<Portfolio> findByUserId(Integer userId);
}
