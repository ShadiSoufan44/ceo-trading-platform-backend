package com.ceo.trading_platform_backend.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ceo.trading_platform_backend.models.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {

	List<Order> findByClientId(UUID clientId);

	// TODO: define if want findby status for after hours (dont need anymore with kafka queues?)
	// OrderStatusChange findByStatus(OrderStatus status);

	// find all orders of with that portfolioid valie in portfolioId column
	List<Order> findByPortfolioId(UUID portfolioId);

}
