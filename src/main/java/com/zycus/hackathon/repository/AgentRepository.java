package com.zycus.hackathon.repository;

import com.zycus.hackathon.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentRepository extends JpaRepository<Agent, String> {

    List<Agent> findByStatus(Agent.Status status);
}
