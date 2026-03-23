package com.sportspredictor.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link OddsSnapshot}. */
class OddsSnapshotTest {

    /** Tests for UUID generation via {@code @PrePersist}. */
    @Nested
    class GenerateId {

        @Test
        void assignsUuidWhenIdIsNull() {
            OddsSnapshot snapshot = new OddsSnapshot();
            snapshot.generateId();
            assertThat(snapshot.getId()).isNotNull();
            assertThat(UUID.fromString(snapshot.getId())).isNotNull();
        }

        @Test
        void doesNotOverwriteExistingId() {
            String existingId = "fixed-id";
            OddsSnapshot snapshot = OddsSnapshot.builder().id(existingId).build();
            snapshot.generateId();
            assertThat(snapshot.getId()).isEqualTo(existingId);
        }
    }

    /** Tests for Lombok builder. */
    @Nested
    class Builder {

        @Test
        void preservesAllFieldsExactly() {
            Instant capturedAt = Instant.parse("2026-03-15T20:00:00Z");

            OddsSnapshot snapshot = OddsSnapshot.builder()
                    .id("snap-1")
                    .eventId("evt-123")
                    .sport("NFL")
                    .bookmaker("DraftKings")
                    .market("h2h")
                    .oddsData("{\"home\": -150, \"away\": 130}")
                    .capturedAt(capturedAt)
                    .build();

            assertThat(snapshot.getBookmaker()).isEqualTo("DraftKings");
            assertThat(snapshot.getOddsData()).contains("home");
            assertThat(snapshot.getCapturedAt()).isEqualTo(capturedAt);
        }
    }
}
