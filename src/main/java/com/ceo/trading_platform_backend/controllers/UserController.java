package com.ceo.trading_platform_backend.controllers;

import com.ceo.trading_platform_backend.dto.ChangePasswordRequest;
import com.ceo.trading_platform_backend.dto.CreateUserRequest;
import com.ceo.trading_platform_backend.dto.LoginRequest;
import com.ceo.trading_platform_backend.dto.LoginResponse;
import com.ceo.trading_platform_backend.dto.UserResponse;
import com.ceo.trading_platform_backend.services.AuthenticationService;
import com.ceo.trading_platform_backend.services.PasswordService;
import com.ceo.trading_platform_backend.services.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final PasswordService passwordService;

    public UserController(UserService userService, AuthenticationService authenticationService, PasswordService passwordService) {
        this.userService = userService;
        this.authenticationService = authenticationService;
        this.passwordService = passwordService;
    }

    // ============ PUBLIC ENDPOINTS ============

    @PostMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // TODO: Implement login endpoint
        // Calls authenticationService.login(request)
        // Returns LoginResponse with JWT token
        return null;
    }

    // ============ CLIENT ENDPOINTS ============

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userService.isOwnProfile(#userId)")
    public ResponseEntity<UserResponse> getUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @PostMapping("/{userId}/change-password")
    @PreAuthorize("hasRole('ADMIN') or @userService.isOwnProfile(#userId)")
    public ResponseEntity<Void> changePassword(
            @PathVariable Integer userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        passwordService.changePassword(userId, request);
        return ResponseEntity.ok().build();
    }

    // ============ ADMIN ENDPOINTS ============
    /*
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Integer userId,
            @Valid @RequestBody CreateUserRequest request) {
        // TODO: Implement update endpoint
        return null;
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> suspendUser(@PathVariable Integer userId) {
        // TODO: Implement suspend endpoint
        return ResponseEntity.ok().build();
    }
    */

    // ============ ANALYST ENDPOINTS ============
    /* 
    @GetMapping("/reporting/volume")
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<?> getVolumeReport() {
        // TODO: Implement volume report
        // Persona: Priya - monthly trading volumes
        return ResponseEntity.ok("{}");
    }

    @GetMapping("/reporting/activity")
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<?> getActivityReport() {
        // TODO: Implement activity report
        // Persona: Priya - client activity metrics
        return ResponseEntity.ok("{}");
    }

    @GetMapping("/analytics/trades")
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<?> getTradeAnalytics() {
        // TODO: Implement trade analytics
        // Persona: Priya - historical trade data (no PII)
        return ResponseEntity.ok("{}");
    }
        */
} 
