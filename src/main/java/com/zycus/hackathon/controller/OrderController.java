package com.zycus.hackathon.controller;

import com.zycus.hackathon.dto.CreateOrderRequest;
import com.zycus.hackathon.entity.Agent;
import com.zycus.hackathon.entity.Order;
import com.zycus.hackathon.entity.ReassignmentSuggestion;
import com.zycus.hackathon.routing.AgentRecommendation;
import com.zycus.hackathon.routing.RoutingContext;
import com.zycus.hackathon.service.AgentService;
import com.zycus.hackathon.service.OrderService;
import com.zycus.hackathon.service.RoutingService;
import com.zycus.hackathon.service.SuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final AgentService agentService;
    private final RoutingService routingService;
    private final SuggestionService suggestionService;

    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order saved = orderService.createOrder(request.id(), request.description(), request.assignedAgentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(@RequestParam(required = false) String status) {
        Order.Status orderStatus = status != null ? Order.Status.valueOf(status) : null;
        return ResponseEntity.ok(orderService.getOrders(orderStatus));
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable String id) {
        return orderService.getOrder(id);
    }

    @PostMapping("/{id}/suggest")
    public ResponseEntity<ReassignmentSuggestion> suggest(@PathVariable String id) {
        Order order = orderService.getOrder(id);
        List<Agent> availableAgents = agentService.getAvailableAgents();

        RoutingContext context = new RoutingContext(ReassignmentSuggestion.TriggerReason.INITIAL, null);
        List<AgentRecommendation> recommendations = routingService.recommend(order, availableAgents, context);

        if (recommendations.isEmpty()) {
            throw new IllegalArgumentException("No agents available for routing");
        }

        AgentRecommendation topPick = recommendations.get(0);

        ReassignmentSuggestion suggestion = new ReassignmentSuggestion();
        suggestion.setOrderId(order.getId());
        suggestion.setRecommendedAgentId(topPick.agentId());
        suggestion.setConfidence(topPick.confidence());
        suggestion.setReasoning(topPick.reasoning());
        suggestion.setStatus(ReassignmentSuggestion.Status.PENDING);
        suggestion.setTriggerReason(ReassignmentSuggestion.TriggerReason.INITIAL);
        suggestion.setCreatedAt(Instant.now());

        ReassignmentSuggestion saved = suggestionService.createSuggestion(suggestion);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
