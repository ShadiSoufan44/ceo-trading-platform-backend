package com.ceo.trading_platform_backend.services;

import com.ceo.trading_platform_backend.enums.UserRole;
import com.ceo.trading_platform_backend.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTests {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "01234567890123456789012345678901", 900000L);
        user = mock(User.class);
        when(user.getUserId()).thenReturn(UUID.randomUUID());
        when(user.getRole()).thenReturn(UserRole.CLIENT);
    }

    @Test
    void tokenContainsUserIdAndRole() {
        String token = jwtService.createAccessToken(user);

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractRole(token)).isEqualTo("CLIENT");
        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getUserId());
    }

    @Test
    void invalidTokenIsRejected() {
        assertThat(jwtService.isValid("not-a-jwt")).isFalse();
    }
}