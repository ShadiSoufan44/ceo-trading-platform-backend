package com.ceo.trading_platform_backend.services;

import com.ceo.trading_platform_backend.dto.LoginRequest;
import com.ceo.trading_platform_backend.dto.LoginResponse;
import com.ceo.trading_platform_backend.dto.RefreshTokenRequest;
import com.ceo.trading_platform_backend.dto.AuthenticationResult;
import com.ceo.trading_platform_backend.dto.ChangePasswordRequest;
import com.ceo.trading_platform_backend.dto.ChangePasswordResponse;
import com.ceo.trading_platform_backend.dto.LogoutResponse;
import com.ceo.trading_platform_backend.exception.ResourceNotFoundException;
import com.ceo.trading_platform_backend.models.User;
import com.ceo.trading_platform_backend.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordService passwordService;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            PasswordService passwordService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordService = passwordService;
    }

    @Transactional
    public AuthenticationResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResourceNotFoundException("Invalid email or password");
        }

        String refreshToken = refreshTokenService.createRefreshToken(user);
        LoginResponse response = new LoginResponse(
            user.getUserId(),
            user.getEmail(),
            jwtService.createAccessToken(user),
            jwtService.getAccessTokenExpiration(),
            user.getRole().name()
        );
        return new AuthenticationResult(response, refreshToken);
    }

    @Transactional
    public AuthenticationResult refresh(String rawRefreshToken) {
        User user = refreshTokenService.validateRefreshToken(rawRefreshToken);
        refreshTokenService.revokeRefreshToken(rawRefreshToken);

        String refreshToken = refreshTokenService.createRefreshToken(user);
        LoginResponse response = new LoginResponse(
                user.getUserId(),
                user.getEmail(),
                jwtService.createAccessToken(user),
                jwtService.getAccessTokenExpiration(),
                user.getRole().name()
        );
        return new AuthenticationResult(response, refreshToken);
    }

    @Transactional
    public LogoutResponse logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revokeRefreshToken(rawRefreshToken);
        }
        return new LogoutResponse("Logout successful", Instant.now());
    }

    @Transactional
    public ChangePasswordResponse changePassword(UUID userId, ChangePasswordRequest request) {
        passwordService.changePassword(userId, request);
        return new ChangePasswordResponse("Password changed successfully", Instant.now());
    }
}
