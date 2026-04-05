package com.sportspredictor.mcpserver.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportspredictor.mcpserver.entity.Bankroll;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Tests for {@link BankrollRepository}. */
@DataJpaTest
// SQLite is the only JDBC driver on the classpath; there is no embedded DB to replace with.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BankrollRepositoryTest {

    @Autowired
    private BankrollRepository repository;

    private Bankroll saveBankroll(String name, Instant createdAt, Instant archivedAt) {
        return repository.saveAndFlush(TestFixtures.bankroll()
                .name(name)
                .createdAt(createdAt)
                .archivedAt(archivedAt)
                .build());
    }

    @Nested
    class FindByName {

        @Test
        void returnsMatchingBankroll() {
            saveBankroll("Main Bankroll", Instant.parse("2026-01-01T00:00:00Z"), null);

            Optional<Bankroll> result = repository.findByName("Main Bankroll");

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Main Bankroll");
        }

        @Test
        void returnsEmptyWhenNoMatch() {
            Optional<Bankroll> result = repository.findByName("Nonexistent");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindByArchivedAtIsNull {

        @Test
        void returnsOnlyActiveBankrolls() {
            saveBankroll("Active", Instant.parse("2026-01-01T00:00:00Z"), null);
            saveBankroll("Archived", Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-03-01T00:00:00Z"));

            List<Bankroll> result = repository.findByArchivedAtIsNull();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Active");
        }
    }

    @Nested
    class FindByArchivedAtIsNotNull {

        @Test
        void returnsOnlyArchivedBankrolls() {
            saveBankroll("Active", Instant.parse("2026-01-01T00:00:00Z"), null);
            saveBankroll("Archived", Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-03-01T00:00:00Z"));

            List<Bankroll> result = repository.findByArchivedAtIsNotNull();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Archived");
        }
    }

    @Nested
    class FindByCreatedAtBetween {

        @Test
        void returnsOnlyBankrollsInRange() {
            saveBankroll("Jan", Instant.parse("2026-01-15T00:00:00Z"), null);
            saveBankroll("Feb", Instant.parse("2026-02-15T00:00:00Z"), null);
            saveBankroll("Mar", Instant.parse("2026-03-15T00:00:00Z"), null);

            List<Bankroll> result = repository.findByCreatedAtBetween(
                    Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-02-28T00:00:00Z"));

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Bankroll::getName).containsExactlyInAnyOrder("Jan", "Feb");
        }
    }
}
