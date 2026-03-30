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

/** Stores the closing line odds snapshot for CLV (Closing Line Value) calculations. */
@Entity
@Table(name = "closing_odds")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ClosingOdds extends BaseEntity {

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "sport", nullable = false)
    private String sport;

    @Column(name = "market", nullable = false)
    private String market;

    @Column(name = "closing_odds_data", nullable = false)
    private String closingOddsData;

    @Column(name = "game_start_time", nullable = false, columnDefinition = "TEXT")
    private Instant gameStartTime;

    @Column(name = "captured_at", nullable = false, updatable = false, columnDefinition = "TEXT")
    private Instant capturedAt;
}
