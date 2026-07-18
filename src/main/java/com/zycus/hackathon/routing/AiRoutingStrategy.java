package com.zycus.hackathon.routing;

import com.zycus.hackathon.entity.Agent;
import com.zycus.hackathon.entity.Order;
import com.zycus.hackathon.entity.ReassignmentSuggestion.TriggerReason;
import com.zycus.hackathon.llm.LLMGateway;
import com.zycus.hackathon.llm.LLMResponse;
import com.zycus.hackathon.llm.LLMResponseParser;
import com.zycus.hackathon.llm.PromptBuilder;
import com.zycus.hackathon.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component("ai")
@RequiredArgsConstructor
@Slf4j
public class AiRoutingStrategy implements RoutingStrategy {

    private final LLMGateway llmGateway;
    private final PromptBuilder promptBuilder;
    private final LLMResponseParser parser;
    private final RuleBasedRoutingStrategy ruleBasedRoutingStrategy;
    private final AgentRepository agentRepository;

    @Override
    public List<AgentRecommendation> recommend(Order order, List<Agent> availableAgents, RoutingContext context) {
        // Nothing to route if there are no candidate agents at all
        if (availableAgents.isEmpty()) {
            log.warn("No available agents to route order {}", order.getId());
            return List.of();
        }

        // Build a prompt tailored to why routing was triggered (fresh assignment vs. recovery)
        String prompt = context.triggerReason() == TriggerReason.AGENT_OFFLINE
                ? promptBuilder.buildReplanPrompt(order, availableAgents, context.offlineAgentId())
                : promptBuilder.buildInitialPrompt(order, availableAgents);

        // Call the LLM; any failure (timeout, network, provider error) falls back to rule-based
        String raw;
        try {
            raw = llmGateway.callLLM(prompt);
        } catch (Exception e) {
            log.warn("LLM call failed, falling back to rule-based: {}", e.getMessage());
            return ruleBasedRoutingStrategy.recommend(order, availableAgents, context);
        }

        // Parse the raw text into structured JSON; unparseable output falls back to rule-based
        Optional<LLMResponse> parsed = parser.parse(raw);
        if (parsed.isEmpty()) {
            log.warn("LLM response unparseable, falling back to rule-based");
            return ruleBasedRoutingStrategy.recommend(order, availableAgents, context);
        }
        LLMResponse response = parsed.get();

        // Hallucination check: the recommended agent ID must actually exist in the DB
        if (!agentRepository.existsById(response.agentId())) {
            log.warn("LLM hallucinated agent ID {} not in database, falling back to rule-based", response.agentId());
            return ruleBasedRoutingStrategy.recommend(order, availableAgents, context);
        }

        // The agent must also be in the available candidate list (not BUSY or OFFLINE)
        boolean isInAvailableList = availableAgents.stream().anyMatch(a -> a.getId().equals(response.agentId()));
        if (!isInAvailableList) {
            log.warn("LLM recommended agent {} that is not in the available list, falling back to rule-based", response.agentId());
            return ruleBasedRoutingStrategy.recommend(order, availableAgents, context);
        }

        // Success — trust the LLM's single recommendation
        return List.of(new AgentRecommendation(response.agentId(), response.confidence(), response.reasoning()));
    }

    @Override
    public String getName() {
        return "ai";
    }
}
