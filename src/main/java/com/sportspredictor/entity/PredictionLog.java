package com.sportspredictor.entity;

import com.sportspredictor.entity.enums.PredictionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** A logged prediction for an event, including confidence and eventual outcome. */
@Entity
@Table(name = "prediction_log")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PredictionLog {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "sport", nullable = false)
    private String sport;

    @Enumerated(EnumType.STRING)
    @Column(name = "prediction_type", nullable = false)
    private PredictionType predictionType;

    @Column(name = "predicted_outcome", nullable = false)
    private String predictedOutcome;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "actual_outcome")
    private String actualOutcome;

    @Column(name = "key_factors", nullable = false)
    private String keyFactors;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TEXT")
    private Instant createdAt;

    @Column(name = "settled_at", columnDefinition = "TEXT")
    private Instant settledAt;

    @PrePersist
    void generateId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
