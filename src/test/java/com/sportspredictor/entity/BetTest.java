package com.sportspredictor.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportspredictor.entity.enums.BetStatus;
import com.sportspredictor.entity.enums.BetType;
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
        void buildsWithEnumFields() {
            Bet bet = Bet.builder()
                    .id("bet-1")
                    .betType(BetType.PARLAY)
                    .status(BetStatus.PENDING)
                    .stake(50.0)
                    .odds(-110.0)
                    .potentialPayout(95.45)
                    .sport("NFL")
                    .eventId("evt-123")
                    .description("Chiefs vs Bills spread")
                    .placedAt(Instant.now())
                    .build();

            assertThat(bet.getBetType()).isEqualTo(BetType.PARLAY);
            assertThat(bet.getStatus()).isEqualTo(BetStatus.PENDING);
            assertThat(bet.getActualPayout()).isNull();
            assertThat(bet.getMetadata()).isNull();
        }
    }
}
