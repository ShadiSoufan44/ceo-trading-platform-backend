package com.ceo.trading_platform_backend.repositories;

import java.util.List;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.repositories.specifications.OrderSpecifications;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

	// Preserve the service contract; Spring Data applies these predicates to both page and count queries.
	default Page<Order> searchAdminOrders(UUID userId, String userName, String status,
			Instant fromUtc, Instant toExclusiveUtc, String ticker, Pageable pageable) {
		Specification<Order> filters = Specification.allOf(
				OrderSpecifications.hasClientId(userId),
				OrderSpecifications.clientNameMatches(userName),
				OrderSpecifications.hasCurrentStatus(status == null ? null : OrderStatus.valueOf(status)),
				OrderSpecifications.placedBetween(fromUtc, toExclusiveUtc),
				OrderSpecifications.hasTicker(ticker),
				OrderSpecifications.newestPlacementFirst());
		return findAll(filters, pageable);
	}

	// Fetch histories only AFTER pagination; collection fetch joins must not paginate in memory.
	@Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderHistory WHERE o.orderId IN :orderIds")
	List<Order> findWithHistoryByOrderIdIn(@Param("orderIds") Collection<UUID> orderIds);

	@Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderHistory WHERE o.clientId = :clientId")
	List<Order> findWithHistoryByClientId(@Param("clientId") UUID clientId);

	List<Order> findByClientId(UUID clientId);

	// TODO: define if want findby status for after hours (dont need anymore with kafka queues?)
	// OrderStatusChange findByStatus(OrderStatus status);

	// find all orders of with that portfolioid valie in portfolioId column
	List<Order> findByPortfolioId(UUID portfolioId);

}
