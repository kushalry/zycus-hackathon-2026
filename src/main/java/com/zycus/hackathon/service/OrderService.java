package com.zycus.hackathon.service;

import com.zycus.hackathon.entity.Order;
import com.zycus.hackathon.exception.NotFoundException;
import com.zycus.hackathon.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order createOrder(String id, String description, String assignedAgentId) {
        Order order = new Order();
        order.setId(id);
        order.setDescription(description);
        order.setAssignedAgentId(assignedAgentId);
        order.setStatus(Order.Status.ASSIGNED);
        order.setCreatedAt(Instant.now());

        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrders(Order.Status status) {
        if (status == null) {
            return orderRepository.findAll();
        }
        return orderRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public Order getOrder(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }
}
