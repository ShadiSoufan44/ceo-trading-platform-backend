package com.ceo.trading_platform_backend.security;

import com.ceo.trading_platform_backend.enums.UserRole;
import com.ceo.trading_platform_backend.models.User;
import com.ceo.trading_platform_backend.services.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTests {

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("01234567890123456789012345678901", 900000L);
        filter = new JwtAuthenticationFilter(jwtService);
        user = mock(User.class);
        when(user.getUserId()).thenReturn(UUID.randomUUID());
        when(user.getRole()).thenReturn(UserRole.CLIENT);
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenAuthenticatesUserAndRole() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.createAccessToken(user));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo(user.getUserId().toString());
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_CLIENT");
    }

    @Test
    void missingBearerTokenLeavesRequestUnauthenticated() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}