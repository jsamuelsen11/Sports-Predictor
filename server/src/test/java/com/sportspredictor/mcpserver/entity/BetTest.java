package com.sportspredictor.mcpserver.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportspredictor.mcpserver.entity.enums.BetStatus;
import com.sportspredictor.mcpserver.entity.enums.BetType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link Bet}. */
class BetTest {

    /** Tests for UUID generation via {@code @PrePersist}. */
    @Nested
    class GenerateId {

        @Test
        void assignsUuidWhenIdIsNull() {
            Bet bet = new Bet();
            bet.generateId();
            assertThat(bet.getId()).isNotNull();
            assertThat(UUID.fromString(bet.getId())).isNotNull();
        }

        @Test
        void doesNotOverwriteExistingId() {
            String existingId = "fixed-id";
            Bet bet = Bet.builder().id(existingId).build();
            bet.generateId();
            assertThat(bet.getId()).isEqualTo(existingId);
        }
    }

    /** Tests for Lombok builder and enum fields. */
    @Nested
    class Builder {

        @Test
        void preservesAllFieldsExactly() {
            Instant placedAt = Instant.parse("2026-03-20T14:00:00Z");

            Bet bet = Bet.builder()
                    .id("bet-1")
                    .betType(BetType.PARLAY)
                    .status(BetStatus.PENDING)
                    .stake(new BigDecimal("50.00"))
                    .odds(new BigDecimal("-110"))
                    .potentialPayout(new BigDecimal("95.45"))
                    .sport("NFL")
                    .eventId("evt-123")
                    .description("Chiefs vs Bills spread")
                    .placedAt(placedAt)
                    .build();

            assertThat(bet.getBetType()).isEqualTo(BetType.PARLAY);
            assertThat(bet.getStatus()).isEqualTo(BetStatus.PENDING);
            assertThat(bet.getStake()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(bet.getOdds()).isEqualByComparingTo(new BigDecimal("-110"));
            assertThat(bet.getPotentialPayout()).isEqualByComparingTo(new BigDecimal("95.45"));
            assertThat(bet.getPlacedAt()).isEqualTo(placedAt);
            assertThat(bet.getActualPayout()).isNull();
            assertThat(bet.getSettledAt()).isNull();
            assertThat(bet.getMetadata()).isNull();
        }
    }
}
