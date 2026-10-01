package com.ceo.trading_platform_backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ceo.trading_platform_backend.models.Order;

public interface OrderRepository extends JpaRepository<Order, Integer> {

	List<Order> findByUserId(Integer userId);
}
