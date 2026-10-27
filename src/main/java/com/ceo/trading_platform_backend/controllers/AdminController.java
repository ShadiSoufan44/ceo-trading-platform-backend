package com.ceo.trading_platform_backend.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ceo.trading_platform_backend.dto.AdminOrderResponseDTO;
import com.ceo.trading_platform_backend.dto.OrderHistoryEntryDTO;
import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.services.AdminService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

	private final AdminService adminService;

	public AdminController(AdminService adminService) {
		this.adminService = adminService;
	}


	@GetMapping("/orders")
	public Page<AdminOrderResponseDTO> getOrders(
			@RequestParam(name = "userId", required = false) UUID userId,
			@RequestParam(name = "userName", required = false) String userName,
			@RequestParam(name = "status", required = false) OrderStatus status,
			@RequestParam(name = "from", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(name = "to", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(name = "ticker", required = false) String ticker,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {
		return adminService.getOrders(userId, userName, status, from, to, ticker, page, size);
	}


	@GetMapping("/orders/{orderId}")
	public AdminOrderResponseDTO getOrderById(@PathVariable(name = "orderId") UUID orderId) {
		return adminService.getAdminOrderById(orderId);
	}

	/** All status changes, oldest first; unknown timestamps last. Missing order returns 404. */
	@GetMapping("/orders/{orderId}/history")
	public List<OrderHistoryEntryDTO> getOrderHistory(@PathVariable(name = "orderId") UUID orderId) {
		return adminService.getOrderHistory(orderId);
	}

	/** All status changes across this user's orders, without truncation; no matches returns []. */
	@GetMapping("/users/{userId}/order-history")
	public List<OrderHistoryEntryDTO> getUserOrderHistory(@PathVariable(name = "userId") UUID userId) {
		return adminService.getUserOrderHistory(userId);
	}
}
