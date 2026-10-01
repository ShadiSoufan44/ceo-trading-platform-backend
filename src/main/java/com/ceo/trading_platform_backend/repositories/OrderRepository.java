package com.ceo.trading_platform_backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ceo.trading_platform_backend.models.Order;

public interface OrderRepository extends JpaRepository<Order, Integer> {

	List<Order> findByUserId(Integer userId);

	// TODO: define if want findby status for after hours (dont need anymore with kafka queues?)
	// OrderStatusChange findByStatus(OrderStatus status);

	// find all orders of with that portfolioid valie in portfolioId column
	List<Order> findByPortfolioId(Integer portfolioId);

}
