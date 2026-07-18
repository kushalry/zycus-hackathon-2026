package com.zycus.hackathon.dto;

import com.zycus.hackathon.entity.ReassignmentSuggestion;
import jakarta.validation.constraints.NotNull;

public record UpdateSuggestionStatusRequest(
        @NotNull ReassignmentSuggestion.Status status
) {
}
