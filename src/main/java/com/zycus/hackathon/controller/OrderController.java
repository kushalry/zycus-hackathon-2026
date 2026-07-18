package com.zycus.hackathon.controller;

import com.zycus.hackathon.dto.CreateOrderRequest;
import com.zycus.hackathon.entity.Order;
import com.zycus.hackathon.repository.OrderRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderRepository orderRepository;

    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order order = new Order();
        order.setId(request.id());
        order.setDescription(request.description());
        order.setAssignedAgentId(request.assignedAgentId());
        order.setStatus(Order.Status.ASSIGNED);
        order.setCreatedAt(Instant.now());

        Order saved = orderRepository.save(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(@RequestParam(required = false) String status) {
        if (status != null) {
            Order.Status orderStatus = Order.Status.valueOf(status);
            return ResponseEntity.ok(orderRepository.findByStatus(orderStatus));
        }
        return ResponseEntity.ok(orderRepository.findAll());
    }
}
