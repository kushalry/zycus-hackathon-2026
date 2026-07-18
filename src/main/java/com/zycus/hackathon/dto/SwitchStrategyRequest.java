package com.zycus.hackathon.dto;

import jakarta.validation.constraints.NotBlank;

public record SwitchStrategyRequest(
        @NotBlank String strategy
) {
}
