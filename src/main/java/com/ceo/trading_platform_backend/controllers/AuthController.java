package com.ceo.trading_platform_backend.controllers;

import com.ceo.trading_platform_backend.dto.LoginRequest;
import com.ceo.trading_platform_backend.dto.LoginResponse;
import com.ceo.trading_platform_backend.dto.AuthenticationResult;
import com.ceo.trading_platform_backend.dto.ChangePasswordRequest;
import com.ceo.trading_platform_backend.dto.ChangePasswordResponse;
import com.ceo.trading_platform_backend.dto.LogoutResponse;
import com.ceo.trading_platform_backend.dto.UserResponse;
import com.ceo.trading_platform_backend.services.AuthenticationService;
import com.ceo.trading_platform_backend.services.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CookieValue;

import java.time.Duration;

/**
 * AuthController handles authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final long refreshTokenExpirationSeconds;

    public AuthController(
            AuthenticationService authenticationService,
            UserService userService,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpirationSeconds) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    /**
     * Authenticate user and return JWT token.
     */
    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticationResult result = authenticationService.login(request);
        return withRefreshTokenCookie(result);
    }

    @PostMapping("/refresh")
    @PreAuthorize("permitAll()")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {
        AuthenticationResult result = authenticationService.refresh(refreshToken);
        return withRefreshTokenCookie(result);
    }

    @PostMapping("/logout")
    @PreAuthorize("permitAll()")
    public ResponseEntity<LogoutResponse> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {
        LogoutResponse response = authenticationService.logout(refreshToken);
        return ResponseEntity.ok()
                .header("Set-Cookie", clearRefreshTokenCookie().toString())
                .body(response);
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChangePasswordResponse> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(authenticationService.changePassword(userId, request));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> currentUser(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(userService.getUser(userId));
    }

    private ResponseEntity<LoginResponse> withRefreshTokenCookie(AuthenticationResult result) {
        return ResponseEntity.ok()
                .header("Set-Cookie", refreshTokenCookie(result.refreshToken()).toString())
                .body(result.response());
    }

    private ResponseCookie refreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ofSeconds(refreshTokenExpirationSeconds))
                .build();
    }

    private ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();
    }



}
