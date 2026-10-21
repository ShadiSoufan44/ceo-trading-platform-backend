package com.ceo.trading_platform_backend.messaging;

import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ceo.trading_platform_backend.dto.OrderStatusUpdateDTO;
import com.ceo.trading_platform_backend.enums.OrderStatus;

@Service
public class OrderStatusNotifier {

    public static final String ORDER_TOPIC_PREFIX = "/topic/orders/";

    private static final List<OrderStatus> TERMINAL_STATUSES = List.of(
        OrderStatus.FULFILLED,
        OrderStatus.REJECTED,
        OrderStatus.CANCELLED
    );

    private final SimpMessagingTemplate messagingTemplate;

    public OrderStatusNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // only push once the status change is committed, so clients never see a status that gets rolled back.
    // fallbackExecution covers saves made outside a transaction (e.g. OrderProducers.submit), which are already committed
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        messagingTemplate.convertAndSend(ORDER_TOPIC_PREFIX + event.orderId(), toUpdate(event));
    }

    public static OrderStatusUpdateDTO toUpdate(OrderStatusChangedEvent event) {
        return new OrderStatusUpdateDTO(
            event.orderId(),
            event.status(),
            event.message(),
            event.date().toString(),
            TERMINAL_STATUSES.contains(event.status())
        );
    }
}
