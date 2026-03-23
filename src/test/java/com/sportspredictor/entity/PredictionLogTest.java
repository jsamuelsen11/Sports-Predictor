package com.sportspredictor.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportspredictor.entity.enums.PredictionType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link PredictionLog}. */
class PredictionLogTest {

    /** Tests for UUID generation via {@code @PrePersist}. */
    @Nested
    class GenerateId {

        @Test
        void assignsUuidWhenIdIsNull() {
            PredictionLog log = new PredictionLog();
            log.generateId();
            assertThat(log.getId()).isNotNull();
            assertThat(UUID.fromString(log.getId())).isNotNull();
        }

        @Test
        void doesNotOverwriteExistingId() {
            String existingId = "fixed-id";
            PredictionLog log = PredictionLog.builder().id(existingId).build();
            log.generateId();
            assertThat(log.getId()).isEqualTo(existingId);
        }
    }

    /** Tests for Lombok builder and enum fields. */
    @Nested
    class Builder {

        @Test
        void preservesAllFieldsExactly() {
            Instant createdAt = Instant.parse("2026-03-22T12:00:00Z");

            PredictionLog log = PredictionLog.builder()
                    .id("pred-1")
                    .eventId("evt-123")
                    .sport("NBA")
                    .predictionType(PredictionType.SPREAD)
                    .predictedOutcome("{\"winner\": \"Lakers\"}")
                    .confidence(0.72)
                    .keyFactors("{\"home_advantage\": true}")
                    .createdAt(createdAt)
                    .build();

            assertThat(log.getPredictionType()).isEqualTo(PredictionType.SPREAD);
            assertThat(log.getConfidence()).isEqualTo(0.72);
            assertThat(log.getCreatedAt()).isEqualTo(createdAt);
            assertThat(log.getActualOutcome()).isNull();
            assertThat(log.getSettledAt()).isNull();
        }
    }
}
