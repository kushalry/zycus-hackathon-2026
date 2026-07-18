package com.zycus.hackathon.routing;

import com.zycus.hackathon.entity.Agent;
import com.zycus.hackathon.entity.Order;

import java.util.List;

/**
 * Strategy contract for producing agent recommendations for an order.
 *
 * <p>Implementations must be side-effect free with respect to the passed-in
 * {@link Order} and {@link Agent} instances — they should only read from them
 * and return recommendations, never mutate or persist state. The {@link RoutingContext}
 * tells the strategy why routing is being requested (e.g. an initial assignment vs.
 * a recovery triggered by an agent going offline), so a strategy may choose to reason
 * differently — or exclude an offline agent — based on that context.
 *
 * <p>Recommendations should be returned ordered from most to least preferred, and
 * each implementation is registered as a Spring bean under a unique, stable name
 * (via {@code @Component(name)}) so it can be selected at runtime by {@code getName()}.
 */
public interface RoutingStrategy {

    /**
     * Produce ranked agent recommendations for the given order.
     *
     * @param order            the order needing an agent
     * @param availableAgents  the candidate pool of agents to consider
     * @param context          why routing is being requested (initial vs. recovery)
     * @return recommendations ordered from most to least preferred; may be empty
     */
    List<AgentRecommendation> recommend(Order order, List<Agent> availableAgents, RoutingContext context);

    /**
     * @return a unique, short, stable name identifying this strategy (e.g. "rule-based", "ai")
     */
    String getName();
}
