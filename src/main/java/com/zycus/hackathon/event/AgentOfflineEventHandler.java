package com.zycus.hackathon.event;

import com.zycus.hackathon.entity.Agent;
import com.zycus.hackathon.entity.Order;
import com.zycus.hackathon.entity.ReassignmentSuggestion;
import com.zycus.hackathon.repository.AgentRepository;
import com.zycus.hackathon.repository.OrderRepository;
import com.zycus.hackathon.repository.ReassignmentSuggestionRepository;
import com.zycus.hackathon.routing.AgentRecommendation;
import com.zycus.hackathon.routing.RoutingContext;
import com.zycus.hackathon.service.RoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgentOfflineEventHandler {

    private final OrderRepository orderRepository;
    private final ReassignmentSuggestionRepository suggestionRepository;
    private final AgentRepository agentRepository;
    private final RoutingService routingService;

    @EventListener
    @Async
    public void handleAgentWentOffline(AgentWentOfflineEvent event) {
        log.info("Async re-plan triggered for offline agent: {}", event.agentId());

        // Find all orders currently assigned to the offline agent
        List<Order> orphanedOrders = orderRepository.findByAssignedAgentIdAndStatus(event.agentId(), Order.Status.ASSIGNED);
        log.info("Found {} orphaned orders for agent {}", orphanedOrders.size(), event.agentId());

        // Get currently available agents (excluding the one that just went offline)
        List<Agent> availableAgents = agentRepository.findByStatus(Agent.Status.AVAILABLE);

        for (Order order : orphanedOrders) {
            processOrphanedOrder(order, availableAgents, event.agentId());
        }
    }

    private void processOrphanedOrder(Order order, List<Agent> availableAgents, String offlineAgentId) {
        // IDEMPOTENCY CHECK - skip if PENDING AGENT_OFFLINE suggestion already exists
        boolean alreadyQueued = suggestionRepository.existsByOrderIdAndTriggerReasonAndStatus(
                order.getId(),
                ReassignmentSuggestion.TriggerReason.AGENT_OFFLINE,
                ReassignmentSuggestion.Status.PENDING
        );
        if (alreadyQueued) {
            log.info("Skipping order {} - PENDING AGENT_OFFLINE suggestion already exists", order.getId());
            return;
        }

        // Run the active routing strategy with RECOVERY context
        RoutingContext context = new RoutingContext(
                ReassignmentSuggestion.TriggerReason.AGENT_OFFLINE,
                offlineAgentId
        );
        List<AgentRecommendation> recommendations;
        try {
            recommendations = routingService.recommend(order, availableAgents, context);
        } catch (Exception e) {
            log.error("Routing failed during async re-plan for order {}: {}", order.getId(), e.getMessage());
            return;
        }

        if (recommendations.isEmpty()) {
            log.warn("No recommendation available for orphaned order {}", order.getId());
            return;
        }

        AgentRecommendation topPick = recommendations.get(0);

        // Persist the suggestion
        ReassignmentSuggestion suggestion = new ReassignmentSuggestion();
        suggestion.setOrderId(order.getId());
        suggestion.setRecommendedAgentId(topPick.agentId());
        suggestion.setConfidence(topPick.confidence());
        suggestion.setReasoning(topPick.reasoning());
        suggestion.setStatus(ReassignmentSuggestion.Status.PENDING);
        suggestion.setTriggerReason(ReassignmentSuggestion.TriggerReason.AGENT_OFFLINE);
        suggestion.setCreatedAt(Instant.now());
        suggestionRepository.save(suggestion);

        // Update order status to REASSIGNMENT_PENDING
        order.setStatus(Order.Status.REASSIGNMENT_PENDING);
        orderRepository.save(order);

        log.info("Queued AGENT_OFFLINE suggestion for order {} recommending agent {}", order.getId(), topPick.agentId());
    }
}
