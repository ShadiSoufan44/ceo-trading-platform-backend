package com.ceo.trading_platform_backend.services;

import com.ceo.trading_platform_backend.dto.LoginRequest;
import com.ceo.trading_platform_backend.dto.LoginResponse;
import com.ceo.trading_platform_backend.dto.RefreshTokenRequest;
import com.ceo.trading_platform_backend.exception.ResourceNotFoundException;
import com.ceo.trading_platform_backend.models.User;
import com.ceo.trading_platform_backend.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResourceNotFoundException("Invalid email or password");
        }

        return new LoginResponse(
            user.getUserId(),
            user.getEmail(),
            jwtService.createAccessToken(user),
            refreshTokenService.createRefreshToken(user),
            jwtService.getAccessTokenExpiration(),
            user.getRole().name()
        );
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        User user = refreshTokenService.validateRefreshToken(request.refreshToken());
        refreshTokenService.revokeRefreshToken(request.refreshToken());

        return new LoginResponse(
                user.getUserId(),
                user.getEmail(),
                jwtService.createAccessToken(user),
                refreshTokenService.createRefreshToken(user),
                jwtService.getAccessTokenExpiration(),
                user.getRole().name()
        );
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revokeRefreshToken(request.refreshToken());
    }
}
