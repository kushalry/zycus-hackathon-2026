package com.zycus.hackathon.controller;

import com.zycus.hackathon.dto.UpdateAgentStatusRequest;
import com.zycus.hackathon.entity.Agent;
import com.zycus.hackathon.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final AgentService agentService;

    @GetMapping
    public ResponseEntity<List<Agent>> getAgents() {
        return ResponseEntity.ok(agentService.getAllAgents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Agent> getAgent(@PathVariable String id) {
        return ResponseEntity.ok(agentService.getAgent(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Agent> updateAgentStatus(
            @PathVariable String id,
            @RequestBody @Valid UpdateAgentStatusRequest request) {
        Agent saved = agentService.updateStatus(id, request.status());
        return ResponseEntity.ok(saved);
    }
}
