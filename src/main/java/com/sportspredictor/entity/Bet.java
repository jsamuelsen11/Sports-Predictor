package com.sportspredictor.entity;

import com.sportspredictor.entity.enums.BetStatus;
import com.sportspredictor.entity.enums.BetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A single bet placed against a bankroll. */
@Entity
@Table(name = "bet")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Bet extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bankroll_id", nullable = false, updatable = false)
    private Bankroll bankroll;

    @Enumerated(EnumType.STRING)
    @Column(name = "bet_type", nullable = false)
    private BetType betType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BetStatus status;

    @Column(name = "stake", nullable = false)
    private BigDecimal stake;

    @Column(name = "odds", nullable = false)
    private BigDecimal odds;

    @Column(name = "potential_payout", nullable = false)
    private BigDecimal potentialPayout;

    @Column(name = "actual_payout")
    private BigDecimal actualPayout;

    @Column(name = "sport", nullable = false)
    private String sport;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "placed_at", nullable = false, updatable = false, columnDefinition = "TEXT")
    private Instant placedAt;

    @Column(name = "settled_at", columnDefinition = "TEXT")
    private Instant settledAt;

    @Column(name = "metadata")
    private String metadata;

    @Column(name = "parent_bet_id")
    private String parentBetId;

    @Column(name = "expires_at", columnDefinition = "TEXT")
    private Instant expiresAt;
}
