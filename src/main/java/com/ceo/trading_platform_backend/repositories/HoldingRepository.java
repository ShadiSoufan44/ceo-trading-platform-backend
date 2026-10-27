package com.ceo.trading_platform_backend.repositories;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ceo.trading_platform_backend.models.Holding;

public interface HoldingRepository extends JpaRepository<Holding, UUID> {

	List<Holding> findByOrderOrderId(UUID orderId);

	List<Holding> findByOrderOrderIdIn(Collection<UUID> orderIds);
}
