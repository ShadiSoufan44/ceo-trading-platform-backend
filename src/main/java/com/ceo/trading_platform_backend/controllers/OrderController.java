package com.ceo.trading_platform_backend.controllers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ceo.trading_platform_backend.dto.OrderRequestDTO;
import com.ceo.trading_platform_backend.dto.OrderResponseDTO;
import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.enums.Side;
import com.ceo.trading_platform_backend.exception.ResourceNotFoundException;
import com.ceo.trading_platform_backend.messaging.OrderProducers;
import com.ceo.trading_platform_backend.services.InstrumentService;
import com.ceo.trading_platform_backend.services.OrderService;
import com.ceo.trading_platform_backend.models.Order;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;
    private final InstrumentService instrumentService;
    private final OrderProducers producers;


    public OrderController(OrderService orderService, InstrumentService instrumentService, OrderProducers producers) {
        this.orderService = orderService;
        this.instrumentService = instrumentService;
        this.producers = producers;
    }

    //get all orders 
    @GetMapping
    public List <OrderResponseDTO> getAllOrders() {
        List<Order> allOrders = orderService.getAllOrders();
        List<OrderResponseDTO> allResponses = new ArrayList<OrderResponseDTO>();
        for (Order order : allOrders) {
            allResponses.add(createOrderResponse(order));
        }
        return allResponses;
    }
    
    //get orders absed off client id
    @GetMapping("/by_client/{clientID}")
    public List <OrderResponseDTO> getClientOrders(@PathVariable UUID clientID) { 
        List<Order> allOrders = orderService.getClientOrders(clientID);
        List<OrderResponseDTO> allResponses = new ArrayList<OrderResponseDTO>();
        for (Order order : allOrders) {
            allResponses.add(createOrderResponse(order));
        }
        return allResponses;
    }

    @GetMapping("/{orderID}")
    public OrderResponseDTO getOrder(@PathVariable UUID orderID) {
        Optional<Order> order = orderService.getOrderById(orderID);
        OrderResponseDTO response = createOrderResponse(order.orElseThrow(() -> new ResourceNotFoundException("order id " + orderID + " not foudn in DB")));
        return response;
    }

    @PostMapping("/new")
    public ResponseEntity<OrderResponseDTO> submitOrder(@Valid @RequestBody OrderRequestDTO request) {
        // Order order = orderService.submit(request);
        Order order = orderService.createOrder(request);
        producers.submit(order);
        OrderResponseDTO response = createOrderResponse(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    public OrderResponseDTO createOrderResponse(Order order) {
        UUID orderId = order.getOrderId();
        UUID clientId = order.getClientId();
        UUID portfolioId = order.getPortfolioId();
        String instrumentSymbol = instrumentService.getInstrumentById(order.getInstrumentId()).getSymbol();
        String instrumentFullName = instrumentService.getInstrumentById(order.getInstrumentId()).getFullName();
        InstrumentType instrumentType = instrumentService.getInstrumentById(order.getInstrumentId()).getType();
        String createdDate = order.getCreatedDate().toString();
        BigDecimal quantity = order.getQuantity();
        BigDecimal quotedPrice = order.getQuote();
        Side side = order.getSide();
        BigDecimal increaseThreshold = order.getIncreaseThreshold();
        String resolvedDate = "";
        if(isResolved(order)) {
            resolvedDate = order.getCurrentOrderStatus().getDate().toString();
        }
        OrderStatus currentStatus = order.getCurrentOrderStatus().getStatus();
        OrderResponseDTO response = new OrderResponseDTO(orderId, clientId, portfolioId, instrumentSymbol, instrumentType, instrumentFullName, quantity, quotedPrice, side, increaseThreshold, createdDate, resolvedDate, currentStatus); 

        return response;
    }

    private boolean isResolved(Order order) {
        List<OrderStatus> resolvedStatuses = new ArrayList<>();
        resolvedStatuses.add(OrderStatus.CANCELLED);
        resolvedStatuses.add(OrderStatus.REJECTED);
        resolvedStatuses.add(OrderStatus.FULFILLED);
        OrderStatus status = order.getCurrentOrderStatus().getStatus();
        if (resolvedStatuses.contains(status)) {
            return true;
        }
        return false;
    }
}
