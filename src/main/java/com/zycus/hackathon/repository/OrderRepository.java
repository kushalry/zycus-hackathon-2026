package com.zycus.hackathon.repository;

import com.zycus.hackathon.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByStatus(Order.Status status);

    List<Order> findByAssignedAgentIdAndStatus(String agentId, Order.Status status);
}
