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

/** Tracks an odds monitoring subscription for a specific event. */
@Entity
@Table(name = "odds_monitor")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OddsMonitor extends BaseEntity {

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "interval_minutes", nullable = false)
    private int intervalMinutes;

    @Column(name = "duration_hours", nullable = false)
    private int durationHours;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "started_at", nullable = false, updatable = false, columnDefinition = "TEXT")
    private Instant startedAt;

    @Column(name = "expires_at", nullable = false, columnDefinition = "TEXT")
    private Instant expiresAt;
}
