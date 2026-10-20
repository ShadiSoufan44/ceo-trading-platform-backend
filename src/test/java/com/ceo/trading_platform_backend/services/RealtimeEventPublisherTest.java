package com.ceo.trading_platform_backend.services;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.enums.Side;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.OrderStatusChange;

@ExtendWith(MockitoExtension.class)
class RealtimeEventPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private RealtimeEventPublisher realtimeEventPublisher;

    @Test
    void publishOrderUpdateShouldSendUserAndOrderTopicEvents() {
        UUID portfolioId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        UUID orderId = UUID.randomUUID();

        Order order = new Order(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Side.BUY,
            portfolioId,
            clientId,
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(0.02),
            BigDecimal.ONE,
            "America/New_York"
        );

        ReflectionTestUtils.setField(order, "orderId", orderId);
        order.addOrderStatusChange(new OrderStatusChange(OrderStatus.PENDING, "queued", Instant.now()));

        realtimeEventPublisher.publishOrderUpdate(order);

        verify(messagingTemplate, times(1)).convertAndSend(
            org.mockito.ArgumentMatchers.eq("/topic/users/" + clientId + "/orders"),
            org.mockito.ArgumentMatchers.any(Object.class)
        );
        verify(messagingTemplate, times(1)).convertAndSend(
            org.mockito.ArgumentMatchers.eq("/topic/orders/" + orderId),
            org.mockito.ArgumentMatchers.any(Object.class)
        );
    }
}
