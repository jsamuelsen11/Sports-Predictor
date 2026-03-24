package com.sportspredictor.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportspredictor.entity.Bankroll;
import com.sportspredictor.entity.BankrollTransaction;
import com.sportspredictor.entity.enums.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Tests for {@link BankrollTransactionRepository}. */
@DataJpaTest
// SQLite is the only JDBC driver on the classpath; there is no embedded DB to replace with.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BankrollTransactionRepositoryTest {

    @Autowired
    private BankrollTransactionRepository transactionRepository;

    @Autowired
    private BankrollRepository bankrollRepository;

    private Bankroll bankroll;

    @BeforeEach
    void setUp() {
        bankroll = bankrollRepository.saveAndFlush(TestFixtures.bankroll().build());
    }

    private BankrollTransaction saveTransaction(
            TransactionType type, BigDecimal amount, Instant createdAt, String referenceBetId) {
        return transactionRepository.saveAndFlush(TestFixtures.transaction(bankroll)
                .type(type)
                .amount(amount)
                .referenceBetId(referenceBetId)
                .createdAt(createdAt)
                .build());
    }

    @Nested
    class FindByBankrollId {

        @Test
        void returnsTransactionsForBankroll() {
            saveTransaction(
                    TransactionType.DEPOSIT, new BigDecimal("100.00"), Instant.parse("2026-01-10T00:00:00Z"), null);

            List<BankrollTransaction> result = transactionRepository.findByBankrollId(bankroll.getId());

            assertThat(result).hasSize(1);
        }

        @Test
        void returnsEmptyForUnknownBankroll() {
            List<BankrollTransaction> result = transactionRepository.findByBankrollId("unknown");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindByType {

        @Test
        void returnsTransactionsOfType() {
            saveTransaction(
                    TransactionType.DEPOSIT, new BigDecimal("100.00"), Instant.parse("2026-01-10T00:00:00Z"), null);
            saveTransaction(
                    TransactionType.BET_PLACED,
                    new BigDecimal("-50.00"),
                    Instant.parse("2026-01-11T00:00:00Z"),
                    "bet-1");

            List<BankrollTransaction> result = transactionRepository.findByType(TransactionType.DEPOSIT);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(TransactionType.DEPOSIT);
        }
    }

    @Nested
    class FindByBankrollIdAndType {

        @Test
        void filtersByBothBankrollAndType() {
            saveTransaction(
                    TransactionType.DEPOSIT, new BigDecimal("100.00"), Instant.parse("2026-01-10T00:00:00Z"), null);
            saveTransaction(
                    TransactionType.BET_PLACED,
                    new BigDecimal("-50.00"),
                    Instant.parse("2026-01-11T00:00:00Z"),
                    "bet-1");

            List<BankrollTransaction> result =
                    transactionRepository.findByBankrollIdAndType(bankroll.getId(), TransactionType.DEPOSIT);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    class FindByCreatedAtBetween {

        @Test
        void returnsTransactionsInRange() {
            saveTransaction(
                    TransactionType.DEPOSIT, new BigDecimal("100.00"), Instant.parse("2026-01-10T00:00:00Z"), null);
            saveTransaction(
                    TransactionType.DEPOSIT, new BigDecimal("200.00"), Instant.parse("2026-03-10T00:00:00Z"), null);

            List<BankrollTransaction> result = transactionRepository.findByCreatedAtBetween(
                    Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-31T00:00:00Z"));

            assertThat(result).hasSize(1);
        }

        @Test
        void includesTransactionsExactlyAtBoundaries() {
            Instant start = Instant.parse("2026-02-01T00:00:00Z");
            Instant end = Instant.parse("2026-02-01T00:00:02Z");

            BankrollTransaction atStart =
                    saveTransaction(TransactionType.DEPOSIT, new BigDecimal("10.00"), start, null);
            BankrollTransaction inMiddle =
                    saveTransaction(TransactionType.DEPOSIT, new BigDecimal("20.00"), start.plusSeconds(1), null);
            BankrollTransaction atEnd = saveTransaction(TransactionType.DEPOSIT, new BigDecimal("30.00"), end, null);

            List<BankrollTransaction> result = transactionRepository.findByCreatedAtBetween(start, end);

            assertThat(result).containsExactlyInAnyOrder(atStart, inMiddle, atEnd);
        }

        @Test
        void returnsEmptyWhenNoTransactionsInRange() {
            saveTransaction(
                    TransactionType.DEPOSIT, new BigDecimal("100.00"), Instant.parse("2026-06-01T00:00:00Z"), null);

            List<BankrollTransaction> result = transactionRepository.findByCreatedAtBetween(
                    Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-31T00:00:00Z"));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindByReferenceBetId {

        @Test
        void returnsTransactionsForBet() {
            saveTransaction(
                    TransactionType.BET_PLACED,
                    new BigDecimal("-50.00"),
                    Instant.parse("2026-01-10T00:00:00Z"),
                    "bet-123");

            List<BankrollTransaction> result = transactionRepository.findByReferenceBetId("bet-123");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getReferenceBetId()).isEqualTo("bet-123");
        }

        @Test
        void returnsEmptyForNoMatch() {
            List<BankrollTransaction> result = transactionRepository.findByReferenceBetId("nonexistent");

            assertThat(result).isEmpty();
        }
    }
}
