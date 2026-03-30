package com.sportspredictor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Stores output of a Monte Carlo game simulation. */
@Entity
@Table(name = "simulation_result")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SimulationResult extends BaseEntity {

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "sport", nullable = false)
    private String sport;

    @Column(name = "team1_id", nullable = false)
    private String team1Id;

    @Column(name = "team2_id", nullable = false)
    private String team2Id;

    @Column(name = "num_simulations", nullable = false)
    private int numSimulations;

    @Column(name = "result_data", nullable = false)
    private String resultData;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TEXT")
    private Instant createdAt;
}
