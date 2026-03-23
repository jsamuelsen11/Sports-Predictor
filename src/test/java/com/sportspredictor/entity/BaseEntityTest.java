package com.sportspredictor.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link BaseEntity} UUID generation shared by all entities. */
class BaseEntityTest {

    /** Tests for UUID generation via {@code @PrePersist}. */
    @Nested
    class GenerateId {

        @Test
        void assignsValidUuidWhenIdIsNull() {
            Bankroll entity = new Bankroll();
            entity.generateId();
            assertThat(entity.getId()).isNotNull();
            assertThat(UUID.fromString(entity.getId())).isNotNull();
        }

        @Test
        void doesNotOverwriteExistingId() {
            String existingId = "pre-set-id";
            Bankroll entity = Bankroll.builder().id(existingId).build();
            entity.generateId();
            assertThat(entity.getId()).isEqualTo(existingId);
        }

        @Test
        void generatesUniqueIdsAcrossCalls() {
            Bankroll first = new Bankroll();
            Bankroll second = new Bankroll();
            first.generateId();
            second.generateId();
            assertThat(first.getId()).isNotEqualTo(second.getId());
        }
    }
}
