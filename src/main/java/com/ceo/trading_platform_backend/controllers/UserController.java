package com.ceo.trading_platform_backend.controllers;

import com.ceo.trading_platform_backend.dto.ChangePasswordRequest;
import com.ceo.trading_platform_backend.dto.CreateUserRequest;
import com.ceo.trading_platform_backend.dto.UserResponse;
import com.ceo.trading_platform_backend.services.PasswordService;
import com.ceo.trading_platform_backend.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UserController handles shared user authentication and profile endpoints.
 * 
 * API Paths:
 * - POST /api/users - Create new user (signup, defaults to CLIENT role)
 * - GET /api/users/{userId} - Get user profile
 * - POST /api/users/{userId}/change-password - Change password
 * 
 * Note: Login endpoint is in AuthController at POST /api/auth/login
 * Note: Order/position endpoints are in ClientController
 * Note: Report endpoints are in ReportController
 * Note: Admin endpoints are in AdminController
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final PasswordService passwordService;

    public UserController(UserService userService, PasswordService passwordService) {
        this.userService = userService;
        this.passwordService = passwordService;
    }

    /**
     * Create a new user account (signup).
     * All new users default to CLIENT role.
     * 
     * @param request CreateUserRequest with fullName, email, password
     * @return 201 Created with UserResponse
     */
    @PostMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get user profile by ID.
     * Users can view their own profile; ADMIN can view any profile.
     * 
     * @param userId the user ID
     * @return 200 OK with UserResponse
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userService.isOwnProfile(#userId)")
    public ResponseEntity<UserResponse> getUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    /**
     * Change user password.
     * Users can change their own password; ADMIN can change any user's password.
     * 
     * @param userId the user ID
     * @param request ChangePasswordRequest with oldPassword and newPassword
     * @return 200 OK
     */
    @PostMapping("/{userId}/change-password")
    @PreAuthorize("hasRole('ADMIN') or @userService.isOwnProfile(#userId)")
    public ResponseEntity<Void> changePassword(
            @PathVariable Integer userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        passwordService.changePassword(userId, request);
        return ResponseEntity.ok().build();
    }
} 
