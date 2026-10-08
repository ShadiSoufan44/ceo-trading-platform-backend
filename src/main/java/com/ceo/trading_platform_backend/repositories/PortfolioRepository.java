package com.ceo.trading_platform_backend.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ceo.trading_platform_backend.models.Portfolio;


public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {
    
     public List<Portfolio> findByClientUserId(UUID clientId);
}
