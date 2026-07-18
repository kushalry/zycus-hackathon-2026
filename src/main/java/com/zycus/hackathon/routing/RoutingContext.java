package com.zycus.hackathon.routing;

import com.zycus.hackathon.entity.ReassignmentSuggestion.TriggerReason;

public record RoutingContext(TriggerReason triggerReason, String offlineAgentId) {
}
