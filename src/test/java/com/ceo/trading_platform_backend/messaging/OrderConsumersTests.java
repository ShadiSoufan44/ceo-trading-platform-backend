package com.ceo.trading_platform_backend.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.enums.Side;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.OrderStatusChange;
import com.ceo.trading_platform_backend.services.OrderService;

// regression for the validate -> execute race: orders.unexecuted must only be sent once ACCEPTED is committed
@ExtendWith(MockitoExtension.class)
@DisplayName("Order Consumers Tests")
class OrderConsumersTests {

    @Mock
    private KafkaTemplate<String, UUID> kafka;
    @Mock
    private OrderService orderService;

    private final UUID orderId = UUID.randomUUID();

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private OrderConsumers consumers() {
        return new OrderConsumers(kafka, orderService);
    }

    private Order pendingOrder() {
        Order order = new Order(
            UUID.randomUUID(), UUID.randomUUID(), Side.BUY, UUID.randomUUID(), UUID.randomUUID(),
            new BigDecimal("100"), new BigDecimal("0.1"), new BigDecimal("1"), "America/New_York"
        );
        ReflectionTestUtils.setField(order, "orderId", orderId);
        order.addOrderStatusChange(new OrderStatusChange(OrderStatus.PENDING, "order submitted", Instant.now()));
        return order;
    }

    private void validationResultsIn(OrderStatus status) {
        when(orderService.getOrderById(orderId)).thenReturn(Optional.of(pendingOrder()));
        when(orderService.validate(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.addOrderStatusChange(new OrderStatusChange(status, "validated", Instant.now()));
            return order;
        });
    }

    private void kafkaSendSucceeds() {
        when(kafka.send(anyString(), any(UUID.class))).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Nested
    @DisplayName("inside an active transaction synchronization")
    class WithSynchronization {

        @Test
        @DisplayName("ACCEPTED: nothing is sent before commit")
        void acceptedDoesNotSendBeforeCommit() {
            validationResultsIn(OrderStatus.ACCEPTED);
            TransactionSynchronizationManager.initSynchronization();

            consumers().validate(orderId);

            verify(kafka, never()).send(anyString(), any(UUID.class));
            verify(orderService).save(any(Order.class));
            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
        }

        @Test
        @DisplayName("ACCEPTED: sends orders.unexecuted with the order id once afterCommit runs")
        void acceptedSendsAfterCommit() {
            validationResultsIn(OrderStatus.ACCEPTED);
            kafkaSendSucceeds();
            TransactionSynchronizationManager.initSynchronization();

            consumers().validate(orderId);
            verify(kafka, never()).send(anyString(), any(UUID.class));

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            TransactionSynchronizationUtils.invokeAfterCommit(synchronizations);

            verify(kafka, times(1)).send("orders.unexecuted", orderId);
        }

        @Test
        @DisplayName("ACCEPTED: a rolled back transaction never sends")
        void acceptedRolledBackNeverSends() {
            validationResultsIn(OrderStatus.ACCEPTED);
            TransactionSynchronizationManager.initSynchronization();

            consumers().validate(orderId);

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            TransactionSynchronizationUtils.invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_ROLLED_BACK);

            verify(kafka, never()).send(anyString(), any(UUID.class));
        }

        @Test
        @DisplayName("REJECTED: never sends, before or after commit")
        void rejectedNeverSends() {
            validationResultsIn(OrderStatus.REJECTED);
            TransactionSynchronizationManager.initSynchronization();

            consumers().validate(orderId);
            TransactionSynchronizationUtils.invokeAfterCommit(TransactionSynchronizationManager.getSynchronizations());

            verify(kafka, never()).send(anyString(), any(UUID.class));
            assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
        }

        @Test
        @DisplayName("an order that is no longer PENDING is skipped and never sent")
        void nonPendingIsSkipped() {
            Order order = pendingOrder();
            order.addOrderStatusChange(new OrderStatusChange(OrderStatus.ACCEPTED, "already accepted", Instant.now()));
            when(orderService.getOrderById(orderId)).thenReturn(Optional.of(order));
            TransactionSynchronizationManager.initSynchronization();

            consumers().validate(orderId);
            TransactionSynchronizationUtils.invokeAfterCommit(TransactionSynchronizationManager.getSynchronizations());

            verify(orderService, never()).validate(any(Order.class));
            verify(kafka, never()).send(anyString(), any(UUID.class));
        }

        @Test
        @DisplayName("validation throwing marks the order rejected and never sends")
        void validationFailureMarksRejected() {
            when(orderService.getOrderById(orderId)).thenReturn(Optional.of(pendingOrder()));
            when(orderService.validate(any(Order.class))).thenThrow(new IllegalStateException("market api down"));
            TransactionSynchronizationManager.initSynchronization();

            consumers().validate(orderId);
            TransactionSynchronizationUtils.invokeAfterCommit(TransactionSynchronizationManager.getSynchronizations());

            verify(orderService).markRejected(orderId, "order validation failed: market api down");
            verify(kafka, never()).send(anyString(), any(UUID.class));
        }
    }

    @Nested
    @DisplayName("without transaction synchronization")
    class WithoutSynchronization {

        @Test
        @DisplayName("ACCEPTED: sends orders.unexecuted immediately")
        void acceptedSendsImmediately() {
            assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
            validationResultsIn(OrderStatus.ACCEPTED);
            kafkaSendSucceeds();

            consumers().validate(orderId);

            verify(kafka, times(1)).send("orders.unexecuted", orderId);
        }

        @Test
        @DisplayName("REJECTED: never sends")
        void rejectedNeverSends() {
            validationResultsIn(OrderStatus.REJECTED);

            consumers().validate(orderId);

            verify(kafka, never()).send(anyString(), any(UUID.class));
        }
    }
}
