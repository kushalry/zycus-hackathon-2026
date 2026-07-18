package com.zycus.hackathon.routing;

import com.zycus.hackathon.entity.Agent;
import com.zycus.hackathon.entity.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("ai")
public class AiRoutingStrategy implements RoutingStrategy {

    private final RuleBasedRoutingStrategy fallback;

    public AiRoutingStrategy(RuleBasedRoutingStrategy fallback) {
        this.fallback = fallback;
    }

    @Override
    public List<AgentRecommendation> recommend(Order order, List<Agent> availableAgents, RoutingContext context) {
        // LLM wiring comes in T-3
        return List.of();
    }

    @Override
    public String getName() {
        return "ai";
    }
}
