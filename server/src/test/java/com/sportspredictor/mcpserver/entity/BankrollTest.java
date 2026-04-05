package com.sportspredictor.mcpserver.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
        void preservesAllFieldsExactly() {
            Instant createdAt = Instant.parse("2026-01-15T10:00:00Z");
            Instant archivedAt = Instant.parse("2026-03-01T18:30:00Z");

            Bankroll bankroll = Bankroll.builder()
                    .id("abc-123")
                    .name("Season 2026")
                    .startingBalance(new BigDecimal("1000.00"))
                    .currentBalance(new BigDecimal("950.50"))
                    .createdAt(createdAt)
                    .archivedAt(archivedAt)
                    .build();

            assertThat(bankroll.getId()).isEqualTo("abc-123");
            assertThat(bankroll.getName()).isEqualTo("Season 2026");
            assertThat(bankroll.getStartingBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(bankroll.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("950.50"));
            assertThat(bankroll.getCreatedAt()).isEqualTo(createdAt);
            assertThat(bankroll.getArchivedAt()).isEqualTo(archivedAt);
        }
    }
}
