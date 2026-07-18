package com.zycus.hackathon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "orders")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Order {

    @Id
    @EqualsAndHashCode.Include
    private String id;

    @Column(name = "assigned_agent_id")
    private String assignedAgentId;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String zone;

    private String weightClass;

    private Instant slaDeadline;

    private String description;

    @CreationTimestamp
    private Instant createdAt;

    public enum Status {
        ASSIGNED,
        REASSIGNMENT_PENDING,
        REASSIGNED,
        DELIVERED
    }
}
