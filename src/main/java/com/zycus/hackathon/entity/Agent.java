package com.zycus.hackathon.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Agent {

    @Id
    @EqualsAndHashCode.Include
    private String id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Status status;

    private int currentLoad;

    private String zone;

    private Integer capacity;

    public enum Status {
        AVAILABLE,
        BUSY,
        OFFLINE
    }
}
