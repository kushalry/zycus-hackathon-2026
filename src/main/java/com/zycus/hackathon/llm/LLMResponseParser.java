package com.zycus.hackathon.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class LLMResponseParser {

    private final ObjectMapper objectMapper;

    public Optional<LLMResponse> parse(String rawResponse) {
        try {
            String cleaned = stripCodeFences(rawResponse);
            String json = extractJson(cleaned);

            JsonNode node = objectMapper.readTree(json);
            String agentId = node.get("agentId").asText();
            double confidence = node.get("confidence").asDouble();
            String reasoning = node.get("reasoning").asText();

            return Optional.of(new LLMResponse(agentId, confidence, reasoning));
        } catch (Exception e) {
            log.warn("LLM response parse failed: {}", rawResponse, e);
            return Optional.empty();
        }
    }

    private String stripCodeFences(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(json)?", "").trim();
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start == -1 || end == -1 || end < start) {
            throw new IllegalArgumentException("No JSON object found in response");
        }
        return text.substring(start, end + 1);
    }
}
