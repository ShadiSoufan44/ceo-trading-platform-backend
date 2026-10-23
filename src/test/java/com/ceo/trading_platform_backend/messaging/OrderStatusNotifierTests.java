package com.ceo.trading_platform_backend.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.ceo.trading_platform_backend.dto.OrderStatusUpdateDTO;
import com.ceo.trading_platform_backend.enums.OrderStatus;

@DisplayName("Order Status Notifier Tests")
class OrderStatusNotifierTests {

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("onOrderStatusChanged payload")
    class Payload {

        @Mock
        private SimpMessagingTemplate messagingTemplate;

        @ParameterizedTest(name = "{0}")
        @EnumSource(OrderStatus.class)
        @DisplayName("sends to /topic/orders/{id} with status, message, timestamp and terminal flag")
        void sendsUpdateForEveryStatus(OrderStatus status) {
            UUID orderId = UUID.randomUUID();
            Instant date = Instant.parse("2026-10-21T14:30:15.123Z");
            OrderStatusNotifier notifier = new OrderStatusNotifier(messagingTemplate);

            notifier.onOrderStatusChanged(new OrderStatusChangedEvent(orderId, status, "msg for " + status, date));

            ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate, times(1)).convertAndSend(Mockito.eq("/topic/orders/" + orderId), payload.capture());
            assertThat(payload.getValue()).isInstanceOf(OrderStatusUpdateDTO.class);
            OrderStatusUpdateDTO update = (OrderStatusUpdateDTO) payload.getValue();
            assertThat(update.orderId()).isEqualTo(orderId);
            assertThat(update.status()).isEqualTo(status);
            assertThat(update.message()).isEqualTo("msg for " + status);
            assertThat(update.timestamp()).isEqualTo("2026-10-21T14:30:15.123Z");
            boolean expectedTerminal = switch (status) {
                case FULFILLED, REJECTED, CANCELLED -> true;
                case PENDING, ACCEPTED -> false;
            };
            assertThat(update.terminal()).isEqualTo(expectedTerminal);
        }

        @Test
        @DisplayName("topic prefix constant is /topic/orders/")
        void topicPrefix() {
            assertThat(OrderStatusNotifier.ORDER_TOPIC_PREFIX).isEqualTo("/topic/orders/");
        }
    }

    // real Spring event + transaction infrastructure, with a no-op transaction manager (no DB needed)
    @Nested
    @SpringJUnitConfig(AfterCommit.Config.class)
    @DisplayName("after-commit delivery")
    class AfterCommit {

        @Configuration
        @EnableTransactionManagement
        static class Config {
            @Bean
            SimpMessagingTemplate messagingTemplate() {
                return Mockito.mock(SimpMessagingTemplate.class);
            }

            @Bean
            OrderStatusNotifier orderStatusNotifier(SimpMessagingTemplate messagingTemplate) {
                return new OrderStatusNotifier(messagingTemplate);
            }

            @Bean
            PlatformTransactionManager transactionManager() {
                return new NoOpTransactionManager();
            }
        }

        static class NoOpTransactionManager extends AbstractPlatformTransactionManager {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {}

            @Override
            protected void doCommit(DefaultTransactionStatus status) {}

            @Override
            protected void doRollback(DefaultTransactionStatus status) {}
        }

        @Autowired
        private ApplicationEventPublisher publisher;
        @Autowired
        private SimpMessagingTemplate messagingTemplate;
        @Autowired
        private PlatformTransactionManager transactionManager;

        private final UUID orderId = UUID.randomUUID();

        private OrderStatusChangedEvent event() {
            return new OrderStatusChangedEvent(orderId, OrderStatus.ACCEPTED, "order accepted", Instant.now());
        }

        @BeforeEach
        void resetMock() {
            reset(messagingTemplate);
        }

        @Test
        @DisplayName("committed transaction produces exactly one message, and only after commit")
        void commitSendsOnce() {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                publisher.publishEvent(event());
                verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
            });

            verify(messagingTemplate, times(1)).convertAndSend(Mockito.eq("/topic/orders/" + orderId), any(Object.class));
        }

        @Test
        @DisplayName("transaction marked rollback-only produces no message")
        void rollbackOnlySendsNothing() {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                publisher.publishEvent(event());
                status.setRollbackOnly();
            });

            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }

        @Test
        @DisplayName("transaction rolled back by an exception produces no message")
        void exceptionRollbackSendsNothing() {
            assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                publisher.publishEvent(event());
                throw new IllegalStateException("boom");
            })).isInstanceOf(IllegalStateException.class);

            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }

        @Test
        @DisplayName("event published outside a transaction is sent immediately (fallbackExecution)")
        void noTransactionSendsImmediately() {
            publisher.publishEvent(event());

            verify(messagingTemplate, times(1)).convertAndSend(Mockito.eq("/topic/orders/" + orderId), any(Object.class));
        }
    }
}
