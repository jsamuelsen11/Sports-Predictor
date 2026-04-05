package com.sportspredictor.mcpserver.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportspredictor.mcpserver.entity.PredictionLog;
import com.sportspredictor.mcpserver.entity.enums.PredictionType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Tests for {@link PredictionLogRepository}. */
@DataJpaTest
// SQLite is the only JDBC driver on the classpath; there is no embedded DB to replace with.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PredictionLogRepositoryTest {

    @Autowired
    private PredictionLogRepository repository;

    private PredictionLog savePrediction(
            String eventId, String sport, PredictionType type, String actualOutcome, Instant createdAt) {
        return repository.saveAndFlush(TestFixtures.predictionLog()
                .eventId(eventId)
                .sport(sport)
                .predictionType(type)
                .actualOutcome(actualOutcome)
                .createdAt(createdAt)
                .build());
    }

    @Nested
    class FindByEventId {

        @Test
        void returnsPredictionsForEvent() {
            savePrediction("evt-1", "NFL", PredictionType.MONEYLINE, null, Instant.parse("2026-01-10T00:00:00Z"));

            List<PredictionLog> result = repository.findByEventId("evt-1");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEventId()).isEqualTo("evt-1");
        }
    }

    @Nested
    class FindBySport {

        @Test
        void returnsPredictionsForSport() {
            savePrediction("evt-1", "NFL", PredictionType.MONEYLINE, null, Instant.parse("2026-01-10T00:00:00Z"));
            savePrediction("evt-2", "NBA", PredictionType.SPREAD, null, Instant.parse("2026-01-10T00:00:00Z"));

            List<PredictionLog> result = repository.findBySport("NBA");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSport()).isEqualTo("NBA");
        }
    }

    @Nested
    class FindByPredictionType {

        @Test
        void returnsPredictionsOfType() {
            savePrediction("evt-1", "NFL", PredictionType.MONEYLINE, null, Instant.parse("2026-01-10T00:00:00Z"));
            savePrediction("evt-2", "NFL", PredictionType.SPREAD, null, Instant.parse("2026-01-10T00:00:00Z"));

            List<PredictionLog> result = repository.findByPredictionType(PredictionType.SPREAD);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPredictionType()).isEqualTo(PredictionType.SPREAD);
        }
    }

    @Nested
    class FindByCreatedAtBetween {

        @Test
        void returnsPredictionsInRange() {
            savePrediction("evt-1", "NFL", PredictionType.MONEYLINE, null, Instant.parse("2026-01-10T00:00:00Z"));
            savePrediction("evt-2", "NFL", PredictionType.SPREAD, null, Instant.parse("2026-03-10T00:00:00Z"));

            List<PredictionLog> result = repository.findByCreatedAtBetween(
                    Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-31T00:00:00Z"));

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    class FindByActualOutcomeIsNull {

        @Test
        void returnsOnlyUnsettledPredictions() {
            savePrediction("evt-1", "NFL", PredictionType.MONEYLINE, null, Instant.parse("2026-01-10T00:00:00Z"));
            savePrediction("evt-2", "NFL", PredictionType.SPREAD, "Team B won", Instant.parse("2026-01-10T00:00:00Z"));

            List<PredictionLog> result = repository.findByActualOutcomeIsNull();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getActualOutcome()).isNull();
        }
    }
}
