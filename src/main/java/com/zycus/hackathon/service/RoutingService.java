package com.zycus.hackathon.service;

import com.zycus.hackathon.entity.Agent;
import com.zycus.hackathon.entity.Order;
import com.zycus.hackathon.routing.AgentRecommendation;
import com.zycus.hackathon.routing.RoutingContext;
import com.zycus.hackathon.routing.RoutingStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingService {

    private static final String FALLBACK_STRATEGY_NAME = "rule-based";

    private final Map<String, RoutingStrategy> strategies;

    @Value("${routing.strategy:rule-based}")
    private String defaultStrategyName;

    private volatile String activeStrategyName;

    @PostConstruct
    public void init() {
        activeStrategyName = defaultStrategyName;
    }

    public List<AgentRecommendation> recommend(Order order, List<Agent> availableAgents, RoutingContext context) {
        RoutingStrategy strategy = strategies.get(activeStrategyName);
        if (strategy == null) {
            log.error("Routing strategy '{}' not found; falling back to '{}'", activeStrategyName, FALLBACK_STRATEGY_NAME);
            strategy = strategies.get(FALLBACK_STRATEGY_NAME);
        }
        return strategy.recommend(order, availableAgents, context);
    }

    public String getActiveStrategyName() {
        return activeStrategyName;
    }

    public List<String> getAvailableStrategyNames() {
        return new ArrayList<>(strategies.keySet());
    }

    public void setActiveStrategy(String name) {
        if (!strategies.containsKey(name)) {
            throw new IllegalArgumentException("Unknown strategy: " + name + ". Available: " + strategies.keySet());
        }
        log.info("Routing strategy changed from {} to {}", activeStrategyName, name);
        activeStrategyName = name;
    }
}
