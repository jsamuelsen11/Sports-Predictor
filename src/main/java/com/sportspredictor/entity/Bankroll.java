package com.sportspredictor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A named bankroll that tracks a starting and current balance over time. */
@Entity
@Table(name = "bankroll")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Bankroll extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "starting_balance", nullable = false)
    private BigDecimal startingBalance;

    @Column(name = "current_balance", nullable = false)
    private BigDecimal currentBalance;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TEXT")
    private Instant createdAt;

    @Column(name = "archived_at", columnDefinition = "TEXT")
    private Instant archivedAt;
}
