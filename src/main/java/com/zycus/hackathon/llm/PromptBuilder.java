package com.zycus.hackathon.llm;

import com.zycus.hackathon.entity.Agent;
import com.zycus.hackathon.entity.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilder {

    public String buildInitialPrompt(Order order, List<Agent> availableAgents) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a delivery ops routing assistant. Assign the following order to the best available agent.\n\n");
        sb.append("ORDER TO ASSIGN:\n");
        sb.append("- ID: ").append(order.getId()).append("\n");
        sb.append("- Description: ").append(order.getDescription()).append("\n\n");
        sb.append("AVAILABLE AGENTS (currentLoad = active orders):\n");
        appendAgents(sb, availableAgents);
        sb.append("\nDECISION CRITERIA:\n");
        sb.append("- Prefer agents with lower currentLoad (more capacity)\n");
        sb.append("- Only recommend agents whose status is AVAILABLE\n");
        sb.append("- Confidence should be between 0.0 and 1.0 based on how strongly one agent stands out\n\n");
        sb.append("Respond with ONLY valid JSON in this exact schema (no markdown, no code fences, no prose):\n");
        sb.append("{\"agentId\": \"AGT-XXX\", \"confidence\": 0.85, \"reasoning\": \"One-sentence explanation an ops person can act on.\"}");
        return sb.toString();
    }

    public String buildReplanPrompt(Order order, List<Agent> availableAgents, String offlineAgentId) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a delivery ops routing assistant. This is a RECOVERY situation, not a fresh assignment.\n\n");
        sb.append("WHAT HAPPENED:\n");
        sb.append("Agent ").append(offlineAgentId).append(" just went OFFLINE. Their previous assignment for order ")
                .append(order.getId()).append(" is now VOID and must be reassigned to a different agent.\n\n");
        sb.append("ORDER NEEDING REASSIGNMENT:\n");
        sb.append("- ID: ").append(order.getId()).append("\n");
        sb.append("- Description: ").append(order.getDescription()).append("\n");
        sb.append("- Previously assigned to: ").append(offlineAgentId).append(" (now unavailable)\n\n");
        sb.append("CURRENTLY AVAILABLE AGENTS (currentLoad = active orders):\n");
        appendAgents(sb, availableAgents);
        sb.append("\nRECOVERY CRITERIA:\n");
        sb.append("- The offline agent ").append(offlineAgentId).append(" is NOT a valid choice — do not recommend them\n");
        sb.append("- Prefer AVAILABLE agents with lower currentLoad (they have capacity to absorb this)\n");
        sb.append("- This is a service recovery — bias toward operational continuity over perfect optimisation\n");
        sb.append("- Confidence should reflect uncertainty in a recovery context (typically 0.6–0.9 unless a clearly best option exists)\n\n");
        sb.append("Respond with ONLY valid JSON in this exact schema (no markdown, no code fences, no prose):\n");
        sb.append("{\"agentId\": \"AGT-XXX\", \"confidence\": 0.75, \"reasoning\": \"One-sentence explanation an ops person can act on, mentioning this is a recovery.\"}");
        return sb.toString();
    }

    private void appendAgents(StringBuilder sb, List<Agent> agents) {
        for (Agent agent : agents) {
            sb.append("- ").append(agent.getId()).append(" (").append(agent.getName()).append(") — status=")
                    .append(agent.getStatus()).append(", currentLoad=").append(agent.getCurrentLoad()).append("\n");
        }
    }
}
