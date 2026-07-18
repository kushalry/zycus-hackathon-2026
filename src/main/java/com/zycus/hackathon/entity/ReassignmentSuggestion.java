package com.zycus.hackathon.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ReassignmentSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private Long orderId;

    private String recommendedAgentId;

    private double confidence;

    @Lob
    private String reasoning;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private TriggerReason triggerReason;

    @CreationTimestamp
    private Instant createdAt;

    public enum Status {
        PENDING,
        ACCEPTED,
        REJECTED
    }

    public enum TriggerReason {
        INITIAL,
        AGENT_OFFLINE
    }
}
