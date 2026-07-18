package com.zycus.hackathon.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String assignedAgentId;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String zone;

    private String weightClass;

    private Instant slaDeadline;

    public enum Status {
        ASSIGNED,
        REASSIGNMENT_PENDING,
        REASSIGNED,
        DELIVERED
    }
}
