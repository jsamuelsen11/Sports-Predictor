package com.sportspredictor.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportspredictor.entity.enums.TransactionType;
import java.math.BigDecimal;
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
        void preservesAllFieldsExactly() {
            Instant createdAt = Instant.parse("2026-02-10T09:00:00Z");

            BankrollTransaction tx = BankrollTransaction.builder()
                    .id("tx-1")
                    .type(TransactionType.DEPOSIT)
                    .amount(new BigDecimal("500.00"))
                    .balanceAfter(new BigDecimal("1500.00"))
                    .createdAt(createdAt)
                    .build();

            assertThat(tx.getType()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(tx.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(tx.getCreatedAt()).isEqualTo(createdAt);
            assertThat(tx.getReferenceBetId()).isNull();
        }
    }
}
