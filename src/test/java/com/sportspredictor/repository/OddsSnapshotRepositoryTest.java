package com.sportspredictor.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportspredictor.entity.OddsSnapshot;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Tests for {@link OddsSnapshotRepository}. */
@DataJpaTest
// SQLite is the only JDBC driver on the classpath; there is no embedded DB to replace with.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OddsSnapshotRepositoryTest {

    @Autowired
    private OddsSnapshotRepository repository;

    private OddsSnapshot saveSnapshot(
            String eventId, String sport, String bookmaker, String market, Instant capturedAt) {
        return repository.saveAndFlush(TestFixtures.oddsSnapshot()
                .eventId(eventId)
                .sport(sport)
                .bookmaker(bookmaker)
                .market(market)
                .capturedAt(capturedAt)
                .build());
    }

    @Nested
    class FindByEventId {

        @Test
        void returnsSnapshotsForEvent() {
            saveSnapshot("evt-1", "NFL", "DraftKings", "spread", Instant.parse("2026-01-10T00:00:00Z"));
            saveSnapshot("evt-2", "NFL", "DraftKings", "spread", Instant.parse("2026-01-10T00:00:00Z"));

            List<OddsSnapshot> result = repository.findByEventId("evt-1");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEventId()).isEqualTo("evt-1");
        }
    }

    @Nested
    class FindBySport {

        @Test
        void returnsSnapshotsForSport() {
            saveSnapshot("evt-1", "NFL", "DraftKings", "spread", Instant.parse("2026-01-10T00:00:00Z"));
            saveSnapshot("evt-2", "NBA", "FanDuel", "moneyline", Instant.parse("2026-01-10T00:00:00Z"));

            List<OddsSnapshot> result = repository.findBySport("NBA");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSport()).isEqualTo("NBA");
        }
    }

    @Nested
    class FindByEventIdAndBookmaker {

        @Test
        void filtersByEventAndBookmaker() {
            saveSnapshot("evt-1", "NFL", "DraftKings", "spread", Instant.parse("2026-01-10T00:00:00Z"));
            saveSnapshot("evt-1", "NFL", "FanDuel", "spread", Instant.parse("2026-01-10T00:00:00Z"));

            List<OddsSnapshot> result = repository.findByEventIdAndBookmaker("evt-1", "FanDuel");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBookmaker()).isEqualTo("FanDuel");
        }
    }

    @Nested
    class FindByCapturedAtBetween {

        @Test
        void returnsSnapshotsInRange() {
            saveSnapshot("evt-1", "NFL", "DraftKings", "spread", Instant.parse("2026-01-10T00:00:00Z"));
            saveSnapshot("evt-2", "NFL", "DraftKings", "spread", Instant.parse("2026-03-10T00:00:00Z"));

            List<OddsSnapshot> result = repository.findByCapturedAtBetween(
                    Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-31T00:00:00Z"));

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    class FindByEventIdAndMarket {

        @Test
        void filtersByEventAndMarket() {
            saveSnapshot("evt-1", "NFL", "DraftKings", "spread", Instant.parse("2026-01-10T00:00:00Z"));
            saveSnapshot("evt-1", "NFL", "DraftKings", "moneyline", Instant.parse("2026-01-10T00:00:00Z"));

            List<OddsSnapshot> result = repository.findByEventIdAndMarket("evt-1", "moneyline");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMarket()).isEqualTo("moneyline");
        }
    }
}
