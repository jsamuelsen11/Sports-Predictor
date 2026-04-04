package com.sportspredictor.mcpserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Bankroll wagering rules and limits configuration. */
@Entity
@Table(name = "bankroll_rules")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BankrollRules extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bankroll_id", nullable = false, unique = true)
    private Bankroll bankroll;

    @Column(name = "max_bet_units")
    private BigDecimal maxBetUnits;

    @Column(name = "daily_exposure_limit")
    private BigDecimal dailyExposureLimit;

    @Column(name = "stop_loss_threshold")
    private BigDecimal stopLossThreshold;

    @Column(name = "max_parlay_legs")
    private Integer maxParlayLegs;

    @Column(name = "min_confidence")
    private BigDecimal minConfidence;

    @Column(name = "unit_size")
    private BigDecimal unitSize;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TEXT")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TEXT")
    private Instant updatedAt;
}
