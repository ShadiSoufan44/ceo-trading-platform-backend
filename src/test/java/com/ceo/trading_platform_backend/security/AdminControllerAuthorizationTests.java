package com.ceo.trading_platform_backend.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ceo.trading_platform_backend.controllers.AdminController;
import com.ceo.trading_platform_backend.services.AdminService;

class AdminControllerAuthorizationTests {
    private AnnotationConfigApplicationContext context;
    private AdminController controller;
    private AdminService service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        context = new AnnotationConfigApplicationContext(Config.class);
        controller = context.getBean(AdminController.class);
        service = context.getBean(AdminService.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void anonymousCannotSearchOrLookUpOrders() {
        assertThatThrownBy(() -> controller.getOrders(null, null, null, null, null, null, 0, 20))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> controller.getOrderById(UUID.randomUUID()))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        verifyNoInteractions(service);
    }

    @Test
    void clientsAndAnalystsCannotAccessAdminOrders() {
        for (String role : new String[] {"CLIENT", "ANALYST"}) {
            authenticate(role);
            assertThatThrownBy(() -> controller.getOrders(null, null, null, null, null, null, 0, 20))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> controller.getOrderById(UUID.randomUUID()))
                    .isInstanceOf(AccessDeniedException.class);
        }
        verifyNoInteractions(service);
    }

    @Test
    void adminCanSearchAndLookUpOrders() {
        authenticate("ADMIN");
        UUID id = UUID.randomUUID();
        controller.getOrders(null, null, null, null, null, null, 0, 20);
        controller.getOrderById(id);
        verify(service).getOrders(null, null, null, null, null, null, 0, 20);
        verify(service).getAdminOrderById(id);
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                UUID.randomUUID().toString(), "unused", AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Config {
        @Bean
        AdminService adminService() {
            return mock(AdminService.class);
        }

        @Bean
        AdminController adminController(AdminService service) {
            return new AdminController(service);
        }
    }
}