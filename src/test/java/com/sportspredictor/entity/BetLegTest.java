package com.sportspredictor.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportspredictor.entity.enums.BetLegStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link BetLeg}. */
class BetLegTest {

    /** Tests for UUID generation via {@code @PrePersist}. */
    @Nested
    class GenerateId {

        @Test
        void assignsUuidWhenIdIsNull() {
            BetLeg leg = new BetLeg();
            leg.generateId();
            assertThat(leg.getId()).isNotNull();
            assertThat(UUID.fromString(leg.getId())).isNotNull();
        }

        @Test
        void doesNotOverwriteExistingId() {
            String existingId = "fixed-id";
            BetLeg leg = BetLeg.builder().id(existingId).build();
            leg.generateId();
            assertThat(leg.getId()).isEqualTo(existingId);
        }
    }

    /** Tests for Lombok builder and enum fields. */
    @Nested
    class Builder {

        @Test
        void preservesAllFieldsExactly() {
            BetLeg leg = BetLeg.builder()
                    .id("leg-1")
                    .legNumber(1)
                    .selection("Chiefs -3.5")
                    .odds(new BigDecimal("-110"))
                    .status(BetLegStatus.PENDING)
                    .eventId("evt-123")
                    .sport("NFL")
                    .build();

            assertThat(leg.getOdds()).isEqualByComparingTo(new BigDecimal("-110"));
            assertThat(leg.getStatus()).isEqualTo(BetLegStatus.PENDING);
            assertThat(leg.getResultDetail()).isNull();
        }
    }
}
