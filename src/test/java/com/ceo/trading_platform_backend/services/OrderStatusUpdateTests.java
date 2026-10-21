package com.ceo.trading_platform_backend.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ceo.trading_platform_backend.controllers.OrderStatusController;
import com.ceo.trading_platform_backend.dto.OrderStatusUpdateDTO;
import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.enums.Side;
import com.ceo.trading_platform_backend.exception.ResourceNotFoundException;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.OrderStatusChange;
import com.ceo.trading_platform_backend.repositories.OrderRepository;

// snapshot returned to a STOMP SUBSCRIBE on /app/orders/{orderId}
@ExtendWith(MockitoExtension.class)
@DisplayName("Order Status Snapshot Tests")
class OrderStatusUpdateTests {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private MarketService marketService;
    @Mock
    private InstrumentService instrumentService;
    @Mock
    private PortfolioService portfolioService;

    @InjectMocks
    private OrderService orderService;

    private final UUID orderId = UUID.randomUUID();

    private Order orderWithHistory(OrderStatusChange... history) {
        Order order = new Order(
            UUID.randomUUID(), UUID.randomUUID(), Side.BUY, UUID.randomUUID(), UUID.randomUUID(),
            new BigDecimal("100"), new BigDecimal("0.1"), new BigDecimal("1"), "America/New_York"
        );
        ReflectionTestUtils.setField(order, "orderId", orderId);
        for (OrderStatusChange change : history) {
            order.addOrderStatusChange(change);
        }
        return order;
    }

    @Test
    @DisplayName("returns the latest status in the order history")
    void returnsLatestStatus() {
        Instant fulfilledAt = Instant.parse("2026-10-21T14:30:02Z");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(orderWithHistory(
            new OrderStatusChange(OrderStatus.PENDING, "order submitted", Instant.parse("2026-10-21T14:30:00Z")),
            new OrderStatusChange(OrderStatus.ACCEPTED, "order accepted", Instant.parse("2026-10-21T14:30:01Z")),
            new OrderStatusChange(OrderStatus.FULFILLED, "order fulfilled", fulfilledAt)
        )));

        OrderStatusUpdateDTO update = orderService.getCurrentStatusUpdate(orderId);

        assertThat(update.orderId()).isEqualTo(orderId);
        assertThat(update.status()).isEqualTo(OrderStatus.FULFILLED);
        assertThat(update.message()).isEqualTo("order fulfilled");
        assertThat(update.timestamp()).isEqualTo(fulfilledAt.toString());
        assertThat(update.terminal()).isTrue();
    }

    @Test
    @DisplayName("a still-PENDING order returns PENDING, not terminal")
    void returnsPending() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(orderWithHistory(
            new OrderStatusChange(OrderStatus.PENDING, "order submitted", Instant.now())
        )));

        OrderStatusUpdateDTO update = orderService.getCurrentStatusUpdate(orderId);

        assertThat(update.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(update.terminal()).isFalse();
    }

    @Test
    @DisplayName("unknown order id throws ResourceNotFoundException")
    void unknownIdThrows() {
        UUID unknown = UUID.randomUUID();
        when(orderRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getCurrentStatusUpdate(unknown))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining(unknown.toString());
    }

    @Test
    @DisplayName("subscribe controller returns the service snapshot")
    void controllerDelegatesToService() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(orderWithHistory(
            new OrderStatusChange(OrderStatus.PENDING, "order submitted", Instant.now()),
            new OrderStatusChange(OrderStatus.ACCEPTED, "order accepted", Instant.now())
        )));

        OrderStatusUpdateDTO update = new OrderStatusController(orderService).currentStatus(orderId);

        assertThat(update.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(update.terminal()).isFalse();
    }
}
