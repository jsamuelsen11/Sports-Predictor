package com.sportspredictor.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link Bankroll}. */
class BankrollTest {

    /** Tests for UUID generation via {@code @PrePersist}. */
    @Nested
    class GenerateId {

        @Test
        void assignsUuidWhenIdIsNull() {
            Bankroll bankroll = new Bankroll();
            bankroll.generateId();
            assertThat(bankroll.getId()).isNotNull();
            assertThat(UUID.fromString(bankroll.getId())).isNotNull();
        }

        @Test
        void doesNotOverwriteExistingId() {
            String existingId = "fixed-id";
            Bankroll bankroll = Bankroll.builder().id(existingId).build();
            bankroll.generateId();
            assertThat(bankroll.getId()).isEqualTo(existingId);
        }
    }

    /** Tests for Lombok builder integration. */
    @Nested
    class Builder {

        @Test
        void buildsWithAllFields() {
            Instant now = Instant.now();
            Bankroll bankroll = Bankroll.builder()
                    .id("abc-123")
                    .name("Season 2026")
                    .startingBalance(1000.0)
                    .currentBalance(950.0)
                    .createdAt(now)
                    .archivedAt(null)
                    .build();

            assertThat(bankroll.getId()).isEqualTo("abc-123");
            assertThat(bankroll.getName()).isEqualTo("Season 2026");
            assertThat(bankroll.getStartingBalance()).isEqualTo(1000.0);
            assertThat(bankroll.getCurrentBalance()).isEqualTo(950.0);
            assertThat(bankroll.getCreatedAt()).isEqualTo(now);
            assertThat(bankroll.getArchivedAt()).isNull();
        }
    }
}
