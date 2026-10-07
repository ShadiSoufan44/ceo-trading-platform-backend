package com.ceo.trading_platform_backend.services;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.repositories.OrderRepository;

import com.ceo.trading_platform_backend.exception.ResourceNotFoundException;


@ExtendWith(MockitoExtension.class)
@DisplayName("Order Service Tests")
public class OrderServiceTests {
    
    @Mock // mock repository stubbing
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    
    private final UUID uuid1 = UUID.randomUUID();
    private final UUID uuid2 = UUID.randomUUID();

    // methods to set up testing values
    private Order createTestOrder(UUID orderId) {
        Order order = new Order(
            null, 
            null, 
            null, 
            null, 
            null, 
            null, 
            null
        );
        return order;
    }

    // classes for tests for each method
    @Nested
    @DisplayName("Test getOrderById()")
    class GetOrderByIdTests {

        @Test 
        @DisplayName("Should return correct order if it exists")
        void shouldReturnOrderWhenExists() {
            @Nullable Optional<Order> expectedOrder = Optional.ofNullable(createTestOrder(uuid1));
            when(orderRepository.findById(uuid1)).thenReturn(expectedOrder);

            Optional<Order> actualOrder = orderService.getOrderById(uuid1);

            assertThat(actualOrder).isNotNull().isEqualTo(expectedOrder);
            verify(orderRepository).findById(uuid1);

        }

        @Test
        @DisplayName("Should return ResourceNotFoundExceptions")
        void shouldThrowExceptionWhenNotExists() {
            Optional<Order> emptyOrder = Optional.empty();
            when(orderRepository.findById(uuid2)).thenReturn(emptyOrder);
            Optional<Order> actualOrder = orderService.getOrderById(uuid2);

            assertThat(actualOrder).isNotNull().isEqualTo(emptyOrder);
            verify(orderRepository).findById(uuid2);
        }

    }

    @Nested
    @DisplayName("Test getAllOrders()") 
    class GetAllOrders {

        @Test
        @DisplayName("Should return all orders made")
        void testGetAllOrders() {
            @Nullable Optional<Order> expectedOrder1 = Optional.ofNullable(createTestOrder(uuid1));
            @Nullable Optional<Order> expectedOrder2 = Optional.ofNullable(createTestOrder(uuid2));
            List<Order> expectedOrders = new ArrayList<Order>();
            expectedOrders.add(expectedOrder1.orElse(null));
            expectedOrders.add(expectedOrder2.orElse(null));
            when(orderRepository.findAll()).thenReturn(expectedOrders);

            List<Order> actualOrders = orderService.getAllOrders();
            assertThat(actualOrders).isNotNull().isEqualTo(expectedOrders);
            verify(orderRepository).findAll();
        }

        void testGetAllOrdersWhenEmpty() {
            List<Order> expectedOrders = new ArrayList<Order>();
            when(orderRepository.findAll()).thenReturn(expectedOrders);

            List<Order> actualOrders = orderService.getAllOrders();
            assertThat(actualOrders).isNotNull().isEqualTo(expectedOrders);
            verify(orderRepository).findAll();
        }

    }

    @Nested
    @DisplayName("Test getClientOrders()")
    class GetClientOrders {
        
    }

}
