package com.sportspredictor.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportspredictor.entity.enums.TransactionType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link BankrollTransaction}. */
class BankrollTransactionTest {

    /** Tests for UUID generation via {@code @PrePersist}. */
    @Nested
    class GenerateId {

        @Test
        void assignsUuidWhenIdIsNull() {
            BankrollTransaction tx = new BankrollTransaction();
            tx.generateId();
            assertThat(tx.getId()).isNotNull();
            assertThat(UUID.fromString(tx.getId())).isNotNull();
        }

        @Test
        void doesNotOverwriteExistingId() {
            String existingId = "fixed-id";
            BankrollTransaction tx =
                    BankrollTransaction.builder().id(existingId).build();
            tx.generateId();
            assertThat(tx.getId()).isEqualTo(existingId);
        }
    }

    /** Tests for Lombok builder and enum fields. */
    @Nested
    class Builder {

        @Test
        void buildsWithAllFields() {
            BankrollTransaction tx = BankrollTransaction.builder()
                    .id("tx-1")
                    .type(TransactionType.DEPOSIT)
                    .amount(500.0)
                    .balanceAfter(1500.0)
                    .createdAt(Instant.now())
                    .build();

            assertThat(tx.getType()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(tx.getReferenceBetId()).isNull();
        }
    }
}
