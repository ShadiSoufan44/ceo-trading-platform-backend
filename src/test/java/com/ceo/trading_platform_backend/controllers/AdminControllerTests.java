package com.ceo.trading_platform_backend.controllers;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.exception.GlobalExceptionHandler;
import com.ceo.trading_platform_backend.exception.ResourceNotFoundException;
import com.ceo.trading_platform_backend.services.AdminService;

/** HTTP binding/delegation tests; standalone MVC does not enable the security filter chain. */
class AdminControllerTests {
    private AdminService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(AdminService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AdminController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void unfilteredRequestUsesDefaultPagination() throws Exception {
        // Match the service's paged response; unpaged Page.empty() has non-serializable offset accessors.
        when(service.getOrders(null, null, null, null, null, null, 0, 20))
            .thenReturn(Page.empty(PageRequest.of(0, 20)));

        mvc.perform(get("/api/admin/orders")).andExpect(status().isOk());

        verify(service).getOrders(null, null, null, null, null, null, 0, 20);
    }

    @Test
    void bindsEveryFilterAndDelegatesToService() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.getOrders(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(Page.empty(PageRequest.of(1, 10)));

        mvc.perform(get("/api/admin/orders").param("userId", id.toString())
                .param("userName", "John").param("status", "ACCEPTED")
                .param("from", "2026-10-01").param("to", "2026-10-26")
                .param("ticker", "AAPL").param("page", "1").param("size", "10"))
                .andExpect(status().isOk());

        verify(service).getOrders(id, "John", OrderStatus.ACCEPTED,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 26), "AAPL", 1, 10);
    }

    @Test
    void malformedParametersReturnBadRequest() throws Exception {
        mvc.perform(get("/api/admin/orders").param("userId", "invalid")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/orders").param("status", "invalid")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/orders").param("from", "not-a-date")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/orders/invalid")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void serviceValidationReturnsBadRequest() throws Exception {
        when(service.getOrders(null, null, null, null, null, null, -1, 20))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be zero or greater"));
        mvc.perform(get("/api/admin/orders").param("page", "-1")).andExpect(status().isBadRequest());
    }

    @Test
    void lookupDelegatesAndMissingOrderReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.getAdminOrderById(id)).thenThrow(new ResourceNotFoundException("Order not found"));

        mvc.perform(get("/api/admin/orders/" + id)).andExpect(status().isNotFound());

        verify(service).getAdminOrderById(id);
    }
}