package com.zycus.hackathon.routing;

import com.zycus.hackathon.entity.Agent;
import com.zycus.hackathon.entity.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component("rule-based")
public class RuleBasedRoutingStrategy implements RoutingStrategy {

    private static final double[] CONFIDENCE_BY_RANK = {1.0, 0.7, 0.4};

    @Override
    public List<AgentRecommendation> recommend(Order order, List<Agent> availableAgents, RoutingContext context) {
        List<Agent> candidates = availableAgents.stream()
                .filter(agent -> agent.getStatus() == Agent.Status.AVAILABLE)
                .sorted(Comparator.comparingInt(Agent::getCurrentLoad))
                .limit(3)
                .toList();

        return java.util.stream.IntStream.range(0, candidates.size())
                .mapToObj(i -> {
                    Agent agent = candidates.get(i);
                    double confidence = CONFIDENCE_BY_RANK[i];
                    String reasoning = "Rule-based: selected " + agent.getId()
                            + " with currentLoad=" + agent.getCurrentLoad() + " (lowest active load)";
                    return new AgentRecommendation(agent.getId(), confidence, reasoning);
                })
                .toList();
    }

    @Override
    public String getName() {
        return "rule-based";
    }
}
