package com.zycus.hackathon.controller;

import com.zycus.hackathon.dto.SwitchStrategyRequest;
import com.zycus.hackathon.service.RoutingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/routing")
@RequiredArgsConstructor
@Slf4j
public class RoutingAdminController {

    private final RoutingService routingService;

    @GetMapping("/strategy")
    public Map<String, Object> getStrategy() {
        return Map.of(
                "activeStrategy", routingService.getActiveStrategyName(),
                "availableStrategies", routingService.getAvailableStrategyNames()
        );
    }

    @PostMapping("/strategy")
    public Map<String, Object> setStrategy(@Valid @RequestBody SwitchStrategyRequest request) {
        routingService.setActiveStrategy(request.strategy());
        return Map.of(
                "activeStrategy", request.strategy(),
                "message", "Strategy switched successfully"
        );
    }
}
