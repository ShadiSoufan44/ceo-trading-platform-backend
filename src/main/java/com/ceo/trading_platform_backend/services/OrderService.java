package com.ceo.trading_platform_backend.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ceo.trading_platform_backend.dto.OrderRequestDTO;
import com.ceo.trading_platform_backend.dto.OrderResponseDTO;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.repositories.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<OrderResponseDTO> getClientOrders(Long clientID) {
        return orderRepository.findAll().stream()
                .filter(order -> order.getUserId() != null && order.getUserId().equals(clientID.intValue()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public OrderResponseDTO getOrder(Long orderID) {
        Order order = orderRepository.findById(orderID.intValue())
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderID));
        return convertToDTO(order);
    }

    public OrderResponseDTO createOrder(OrderRequestDTO request) {
        Order order = new Order(
            nullToZero(request.portfolioId()),
            request.side(),
            nullToZero(request.portfolioId()),
            nullToZero(request.clientId()),
            0, // holdingId - would need actual value
            java.math.BigDecimal.valueOf(nullToZeroDouble(request.quotedPrice())),
            null, // finalPrice
            java.math.BigDecimal.valueOf(nullToZeroDouble(request.wentUpTooMuchThreshold()))
        );
        
        Order savedOrder = orderRepository.save(order);
        return convertToDTO(savedOrder);
    }

    private static int nullToZero(Integer value) {
        if (value == null) {
            return 0;
        }
        return value;
    }

    private static double nullToZeroDouble(Double value) {
        if (value == null) {
            return 0.0;
        }
        return value;
    }

    private OrderResponseDTO convertToDTO(Order order) {
        return new OrderResponseDTO(
                order.getOrderId(),
                order.getUserId(),
                order.getPortfolioId(),
                "", // instrumentSymbol - would need to fetch from Instrument
                null, // instrumentType - would need to fetch from Instrument
                "", // instrumentFullName - would need to fetch from Instrument
                0.0, // quantity
                order.getQuote() != null ? order.getQuote().doubleValue() : 0.0,
                order.getSide(),
                order.getIncreaseThreshold() != null ? order.getIncreaseThreshold().doubleValue() : 0.0,
                "", // createdDate - would need actual timestamp
                "", // resolvedDate - would need actual timestamp
                null // currentStatus - would need OrderStatus enum
        );
    }
}

