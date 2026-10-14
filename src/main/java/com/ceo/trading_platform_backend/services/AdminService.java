package com.ceo.trading_platform_backend.services;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ceo.trading_platform_backend.dto.CreateUserRequest;
import com.ceo.trading_platform_backend.exception.DuplicateResourceException;
import com.ceo.trading_platform_backend.exception.ResourceNotFoundException;
import com.ceo.trading_platform_backend.models.Administrator;
import com.ceo.trading_platform_backend.models.Analyst;
import com.ceo.trading_platform_backend.models.Client;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.User;
import com.ceo.trading_platform_backend.repositories.OrderRepository;
import com.ceo.trading_platform_backend.repositories.UserRepository;

/**
 * AdminService handles admin-only system operations.
 * Responsibilities include user management, system-wide trade queries, and system metrics.
 */
@Service
public class AdminService {
    
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(OrderRepository orderRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Get all trades across the system with optional pagination.
     * Used for trade history auditing and reporting.
     */
    @Transactional(readOnly = true)
    public Page<Order> getAllTrades(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findAll(pageable);
    }

    /**
     * Get a specific trade by ID for dispute resolution and auditing.
     */
    @Transactional(readOnly = true)
    public Order getTradeById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Trade not found with ID: " + orderId));
    }

    /**
     * Get all trades for a specific user/client.
     * Useful for account-level audits and dispute resolution.
     */
    @Transactional(readOnly = true)
    public List<Order> getTradesByClientId(UUID clientId) {
        return orderRepository.findByClientId(clientId);
    }

    /**
     * Get all users in the system with pagination.
     */
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable);
    }

    /**
     * Get a specific user by ID.
     */
    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    /**
     * Get a specific user by email.
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    /**
     * Create a new user (CLIENT, ANALYST, or ADMIN).
     * Admin-only operation for user provisioning.
     */
    @Transactional
    public User createUser(CreateUserRequest request, String role) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }

        User user;
        Instant now = Instant.now();
        String encodedPassword = passwordEncoder.encode(request.password());

        switch (role.toUpperCase()) {
            case "ADMIN":
                user = new Administrator(request.fullName(), request.email(), encodedPassword, now);
                break;
            case "ANALYST":
                user = new Analyst(request.fullName(), request.email(), encodedPassword, now);
                break;
            case "CLIENT":
            default:
                user = new Client(request.fullName(), request.email(), encodedPassword, now);
                break;
        }

        return userRepository.save(user);
    }

    /**
     * Suspend/deactivate a user account.
     * Prevents the user from logging in and accessing their accounts.
     */
    @Transactional
    public void suspendUser(UUID userId) {
        User user = getUserById(userId);
        // TODO: Add a status field to User model to track active/suspended state
        // For now, this is a placeholder for the admin capability
        userRepository.save(user);
    }

    /**
     * Reactivate a suspended user account.
     */
    @Transactional
    public void reactivateUser(UUID userId) {
        User user = getUserById(userId);
        // TODO: Reactivate logic once status field is added to User model
        userRepository.save(user);
    }

    /**
     * Delete a user completely from the system.
     * This is a destructive operation and should require additional confirmation.
     */
    @Transactional
    public void deleteUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }
        userRepository.deleteById(userId);
    }

    /**
     * Get system-wide metrics and statistics.
     * Includes total users, active trades, system health indicators.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> systemMetrics() {
        long totalUsers = userRepository.count();
        long totalOrders = orderRepository.count();
        
        return Map.of(
                "totalUsers", totalUsers,
                "totalOrders", totalOrders,
                "timestamp", OffsetDateTime.now(),
                "status", "healthy"
                // TODO: Add more metrics like:
                // - Active trading volume
                // - System performance metrics
                // - Error rates
                // - User engagement stats
        );
    }

    /**
     * View audit log entries for a specific user.
     * Currently a placeholder - implement with full audit trail system.
     */
    @Transactional(readOnly = true)
    public List<Object> viewAuditLog(UUID userId) {
        // Verify user exists
        getUserById(userId);
        
        // TODO: Implement full audit logging system
        // Should track: user actions, trade modifications, account changes, etc.
        return List.of();
    }
}
