package com.ceo.trading_platform_backend.controllers;

import java.util.List;

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
import com.ceo.trading_platform_backend.services.OrderService;

import com.ceo.trading_platform_backend.services.OrderService;
import com.ceo.trading_platform_backend.uml_objects.Order;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    //get all orders 
    @GetMapping
    public List <OrderResponseDTO> getAllOrders() {
        return orderService.getAllOrders();
    }
    
    //get orders absed off client id
    @GetMapping("/by_client/{clientID}")
    public List <OrderResponseDTO> getClientOrders(@PathVariable Long clientID) { 
        return orderService.getClientOrders(clientID);
    }

    @GetMapping("/{orderID}")
    public OrderResponseDTO getOrder(@PathVariable Long orderID) {
        return orderService.getOrder(orderID);
    }

    @PostMapping 
    public ResponseEntity<OrderResponseDTO> createOrderResponse(@Valid @RequestBody OrderRequestDTO request) {
        // OrderResponseDTO response = orderService.createOrderResponse(request);
        // return ResponseEntity.status(HttpStatus.CREATED).body(response);

        Order order = orderService.submit(request);
        OrderResponseDTO response = orderService.createOrderResponse(order);        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }
    
}
