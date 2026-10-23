package com.ceo.trading_platform_backend.services;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileAuthorizationTests {

    private final UserService service = new UserService(null, null);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatedUserCanAccessOwnProfile() {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        assertThat(service.isOwnProfile(userId)).isTrue();
    }

    @Test
    void authenticatedUserCannotAccessAnotherProfile() {
        authenticate(UUID.randomUUID());
        assertThat(service.isOwnProfile(UUID.randomUUID())).isFalse();
    }

    @Test
    void missingAuthenticationIsDenied() {
        SecurityContextHolder.clearContext();
        assertThat(service.isOwnProfile(UUID.randomUUID())).isFalse();
    }

    private void authenticate(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))));
    }
}