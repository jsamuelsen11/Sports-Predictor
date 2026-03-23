package com.sportspredictor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A named bankroll that tracks a starting and current balance over time. */
@Entity
@Table(name = "bankroll")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Bankroll {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "starting_balance", nullable = false)
    private double startingBalance;

    @Column(name = "current_balance", nullable = false)
    private double currentBalance;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TEXT")
    private Instant createdAt;

    @Column(name = "archived_at", columnDefinition = "TEXT")
    private Instant archivedAt;

    @PrePersist
    void generateId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
