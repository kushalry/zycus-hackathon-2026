package com.zycus.hackathon.service;

import com.zycus.hackathon.entity.Agent;
import com.zycus.hackathon.exception.NotFoundException;
import com.zycus.hackathon.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    private final AgentRepository agentRepository;

    @Transactional(readOnly = true)
    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Agent> getAvailableAgents() {
        return agentRepository.findByStatus(Agent.Status.AVAILABLE);
    }

    @Transactional(readOnly = true)
    public Agent getAgent(String id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Agent not found: " + id));
    }

    @Transactional
    public Agent updateStatus(String id, Agent.Status status) {
        Agent agent = getAgent(id);
        Agent.Status previousStatus = agent.getStatus();
        agent.setStatus(status);
        Agent saved = agentRepository.save(agent);
        log.info("Agent {} status changed from {} to {}", id, previousStatus, status);
        return saved;
    }
}
