package com.zycus.hackathon.llm;

public record LLMResponse(String agentId, double confidence, String reasoning) {
}
