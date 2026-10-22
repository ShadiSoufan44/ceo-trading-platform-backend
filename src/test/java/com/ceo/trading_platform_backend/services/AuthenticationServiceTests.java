package com.ceo.trading_platform_backend.services;

import com.ceo.trading_platform_backend.dto.LoginRequest;
import com.ceo.trading_platform_backend.dto.RefreshTokenRequest;
import com.ceo.trading_platform_backend.enums.UserRole;
import com.ceo.trading_platform_backend.models.User;
import com.ceo.trading_platform_backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordService passwordService;

    private AuthenticationService authenticationService;
    private User user;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
            userRepository, passwordEncoder, jwtService, refreshTokenService, passwordService);
        user = mock(User.class);
        lenient().when(user.getEmail()).thenReturn("test@example.com");
        lenient().when(user.getPasswordHash()).thenReturn("stored-hash");
        lenient().when(user.getUserId()).thenReturn(UUID.randomUUID());
        lenient().when(user.getRole()).thenReturn(UserRole.CLIENT);
    }

    @Test
    void loginReturnsAccessAndRefreshTokens() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "stored-hash")).thenReturn(true);
        when(jwtService.createAccessToken(user)).thenReturn("access-token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-token");

        var result = authenticationService.login(new LoginRequest("test@example.com", "password"));

        assertThat(result.response().accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(passwordEncoder).matches("password", "stored-hash");
    }

    @Test
    void refreshRevokesOldTokenAndRotatesTokens() {
        when(refreshTokenService.validateRefreshToken("old-refresh")).thenReturn(user);
        when(jwtService.createAccessToken(user)).thenReturn("new-access");
        when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(user)).thenReturn("new-refresh");

        var result = authenticationService.refresh(new RefreshTokenRequest("old-refresh").refreshToken());

        assertThat(result.response().accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenService).revokeRefreshToken("old-refresh");
    }

    @Test
    void logoutRevokesRefreshToken() {
        var result = authenticationService.logout("refresh-token");

        assertThat(result.message()).isEqualTo("Logout successful");
        verify(refreshTokenService).revokeRefreshToken("refresh-token");
    }
}