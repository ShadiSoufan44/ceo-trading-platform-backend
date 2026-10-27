package com.ceo.trading_platform_backend.config;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTests {

    private final CorsFilter filter = new CorsFilter(new CorsConfig().corsConfigurationSource(List.of("http://localhost:4200")));

    @Test
    void allowsSignupPreflightWithAuthorizationAndContentType() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/users");
        request.addHeader("Origin", "http://localhost:4200");
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Access-Control-Request-Headers", "authorization, content-type");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:4200");
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
        assertThat(response.getHeader("Access-Control-Allow-Methods")).contains("POST");
    }

    @Test
    void allowsCredentialedRefreshResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/refresh");
        request.addHeader("Origin", "http://localhost:4200");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:4200");
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
    }

    @Test
    void rejectsUnapprovedOrigin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/users");
        request.addHeader("Origin", "https://untrusted.example");
        request.addHeader("Access-Control-Request-Method", "POST");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
    }
}