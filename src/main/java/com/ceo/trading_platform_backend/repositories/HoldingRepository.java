package com.ceo.trading_platform_backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ceo.trading_platform_backend.models.Holding;

public interface HoldingRepository extends JpaRepository<Holding, Integer> {
    
}
