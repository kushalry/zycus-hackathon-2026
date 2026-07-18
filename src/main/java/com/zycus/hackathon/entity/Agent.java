package com.zycus.hackathon.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "agents")   // ← ADD THIS
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Agent {

    @Id
    @EqualsAndHashCode.Include
    private String id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "current_load")   // ← ADD THIS to match SQL column
    private int currentLoad;

    private String zone;

    private Integer capacity;

    public enum Status {
        AVAILABLE,
        BUSY,
        OFFLINE
    }
}