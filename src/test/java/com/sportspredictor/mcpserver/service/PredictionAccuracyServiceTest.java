package com.sportspredictor.mcpserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

import com.sportspredictor.mcpserver.entity.PredictionLog;
import com.sportspredictor.mcpserver.entity.enums.PredictionType;
import com.sportspredictor.mcpserver.repository.PredictionLogRepository;
import com.sportspredictor.mcpserver.service.PredictionAccuracyService.AccuracyResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link PredictionAccuracyService}. */
@ExtendWith(MockitoExtension.class)
class PredictionAccuracyServiceTest {

    @Mock
    private PredictionLogRepository predictionLogRepository;

    @InjectMocks
    private PredictionAccuracyService predictionAccuracyService;

    private static PredictionLog buildPrediction(
            String sport, PredictionType type, double confidence, String predicted, String actual) {
        return PredictionLog.builder()
                .eventId("evt-1")
                .sport(sport)
                .predictionType(type)
                .predictedOutcome(predicted)
                .confidence(confidence)
                .actualOutcome(actual)
                .keyFactors("test factors")
                .createdAt(Instant.now())
                .settledAt(Instant.now())
                .build();
    }

    /** Tests for overall accuracy computation. */
    @Nested
    class OverallAccuracy {

        @Test
        void computesWinRateCorrectly() {
            List<PredictionLog> predictions = List.of(
                    buildPrediction("nba", PredictionType.SPREAD, 0.7, "TEAM_A", "TEAM_A"),
                    buildPrediction("nba", PredictionType.SPREAD, 0.6, "TEAM_A", "TEAM_A"),
                    buildPrediction("nba", PredictionType.SPREAD, 0.8, "TEAM_A", "TEAM_B"));
            when(predictionLogRepository.findByActualOutcomeIsNotNull()).thenReturn(predictions);

            AccuracyResult result = predictionAccuracyService.getAccuracy(null, null, null, null);

            assertThat(result.totalPredictions()).isEqualTo(3);
            assertThat(result.wins()).isEqualTo(2);
            assertThat(result.losses()).isEqualTo(1);
            assertThat(result.winRate()).isCloseTo(0.6667, within(0.001));
        }

        @Test
        void emptyPredictions() {
            when(predictionLogRepository.findByActualOutcomeIsNotNull()).thenReturn(List.of());

            AccuracyResult result = predictionAccuracyService.getAccuracy(null, null, null, null);

            assertThat(result.totalPredictions()).isEqualTo(0);
            assertThat(result.winRate()).isEqualTo(0.0);
        }
    }

    /** Tests for sport filtering. */
    @Nested
    class FilterBySport {

        @Test
        void filtersToRequestedSport() {
            List<PredictionLog> nbaPredictions = List.of(
                    buildPrediction("nba", PredictionType.MONEYLINE, 0.7, "TEAM_A", "TEAM_A"),
                    buildPrediction("nba", PredictionType.MONEYLINE, 0.6, "TEAM_A", "TEAM_B"));
            when(predictionLogRepository.findBySportAndActualOutcomeIsNotNull("nba"))
                    .thenReturn(nbaPredictions);

            AccuracyResult result = predictionAccuracyService.getAccuracy("nba", null, null, null);

            assertThat(result.totalPredictions()).isEqualTo(2);
        }
    }

    /** Tests for prediction type filtering. */
    @Nested
    class FilterByType {

        @Test
        void filtersToRequestedType() {
            List<PredictionLog> predictions = List.of(
                    buildPrediction("nba", PredictionType.SPREAD, 0.7, "TEAM_A", "TEAM_A"),
                    buildPrediction("nba", PredictionType.MONEYLINE, 0.6, "TEAM_A", "TEAM_A"),
                    buildPrediction("nba", PredictionType.SPREAD, 0.8, "TEAM_A", "TEAM_B"));
            when(predictionLogRepository.findByActualOutcomeIsNotNull()).thenReturn(predictions);

            AccuracyResult result = predictionAccuracyService.getAccuracy(null, "SPREAD", null, null);

            assertThat(result.totalPredictions()).isEqualTo(2);
        }
    }

    /** Tests for minimum confidence filtering. */
    @Nested
    class FilterByMinConfidence {

        @Test
        void filtersLowConfidence() {
            List<PredictionLog> predictions = List.of(
                    buildPrediction("nba", PredictionType.SPREAD, 0.5, "TEAM_A", "TEAM_A"),
                    buildPrediction("nba", PredictionType.SPREAD, 0.7, "TEAM_A", "TEAM_A"),
                    buildPrediction("nba", PredictionType.SPREAD, 0.9, "TEAM_A", "TEAM_B"));
            when(predictionLogRepository.findByActualOutcomeIsNotNull()).thenReturn(predictions);

            AccuracyResult result = predictionAccuracyService.getAccuracy(null, null, null, 0.7);

            assertThat(result.totalPredictions()).isEqualTo(2);
        }
    }

    /** Tests for confidence bucket computation. */
    @Nested
    class ConfidenceBuckets {

        @Test
        void bucketsCreatedForPopulatedRanges() {
            List<PredictionLog> predictions = List.of(
                    buildPrediction("nba", PredictionType.SPREAD, 0.55, "TEAM_A", "TEAM_A"),
                    buildPrediction("nba", PredictionType.SPREAD, 0.75, "TEAM_A", "TEAM_A"),
                    buildPrediction("nba", PredictionType.SPREAD, 0.95, "TEAM_A", "TEAM_B"));
            when(predictionLogRepository.findByActualOutcomeIsNotNull()).thenReturn(predictions);

            AccuracyResult result = predictionAccuracyService.getAccuracy(null, null, null, null);

            assertThat(result.confidenceBuckets()).hasSizeGreaterThanOrEqualTo(3);
        }
    }

    /** Tests for sport breakdown grouping. */
    @Nested
    class SportBreakdowns {

        @Test
        void groupsBySport() {
            List<PredictionLog> predictions = List.of(
                    buildPrediction("nba", PredictionType.SPREAD, 0.7, "TEAM_A", "TEAM_A"),
                    buildPrediction("nfl", PredictionType.SPREAD, 0.6, "TEAM_A", "TEAM_A"),
                    buildPrediction("nba", PredictionType.SPREAD, 0.8, "TEAM_A", "TEAM_B"));
            when(predictionLogRepository.findByActualOutcomeIsNotNull()).thenReturn(predictions);

            AccuracyResult result = predictionAccuracyService.getAccuracy(null, null, null, null);

            assertThat(result.sportBreakdowns()).hasSize(2);
        }
    }

    /** Tests for type breakdown grouping. */
    @Nested
    class TypeBreakdowns {

        @Test
        void groupsByType() {
            List<PredictionLog> predictions = List.of(
                    buildPrediction("nba", PredictionType.SPREAD, 0.7, "TEAM_A", "TEAM_A"),
                    buildPrediction("nba", PredictionType.MONEYLINE, 0.6, "TEAM_A", "TEAM_A"),
                    buildPrediction("nba", PredictionType.TOTAL, 0.8, "OVER", "UNDER"));
            when(predictionLogRepository.findByActualOutcomeIsNotNull()).thenReturn(predictions);

            AccuracyResult result = predictionAccuracyService.getAccuracy(null, null, null, null);

            assertThat(result.typeBreakdowns()).hasSize(3);
        }
    }

    /** Tests for summary content. */
    @Nested
    class Summary {

        @Test
        void summaryContainsKeyMetrics() {
            List<PredictionLog> predictions =
                    List.of(buildPrediction("nba", PredictionType.SPREAD, 0.7, "TEAM_A", "TEAM_A"));
            when(predictionLogRepository.findByActualOutcomeIsNotNull()).thenReturn(predictions);

            AccuracyResult result = predictionAccuracyService.getAccuracy(null, null, null, null);

            assertThat(result.summary()).contains("1/1");
            assertThat(result.summary()).contains("100.0%");
        }
    }
}
