package com.sportspredictor.entity;

import com.sportspredictor.entity.enums.BetStatus;
import com.sportspredictor.entity.enums.BetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

/** A single bet placed against a bankroll. */
@Entity
@Table(name = "bet")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Bet {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

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
    private double stake;

    @Column(name = "odds", nullable = false)
    private double odds;

    @Column(name = "potential_payout", nullable = false)
    private double potentialPayout;

    @Column(name = "actual_payout")
    private Double actualPayout;

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

    @PrePersist
    void generateId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
