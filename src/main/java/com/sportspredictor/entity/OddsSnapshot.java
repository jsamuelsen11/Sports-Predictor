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

/** A point-in-time snapshot of odds from a specific bookmaker for an event and market. */
@Entity
@Table(name = "odds_snapshot")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OddsSnapshot extends BaseEntity {

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "sport", nullable = false)
    private String sport;

    @Column(name = "bookmaker", nullable = false)
    private String bookmaker;

    @Column(name = "market", nullable = false)
    private String market;

    @Column(name = "odds_data", nullable = false)
    private String oddsData;

    @Column(name = "captured_at", nullable = false, updatable = false, columnDefinition = "TEXT")
    private Instant capturedAt;
}
