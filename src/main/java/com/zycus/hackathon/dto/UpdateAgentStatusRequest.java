package com.zycus.hackathon.dto;

import com.zycus.hackathon.entity.Agent;
import jakarta.validation.constraints.NotNull;

public record UpdateAgentStatusRequest(
        @NotNull Agent.Status status
) {
}
