package com.sportspredictor.mcpserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sportspredictor.mcpserver.entity.PredictionLog;
import com.sportspredictor.mcpserver.entity.enums.PredictionType;
import com.sportspredictor.mcpserver.repository.PredictionLogRepository;
import com.sportspredictor.mcpserver.service.ConfidenceFactorService.ConfidenceFactorsResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link ConfidenceFactorService}. */
@ExtendWith(MockitoExtension.class)
class ConfidenceFactorServiceTest {

    @Mock
    private PredictionLogRepository predictionLogRepository;

    @InjectMocks
    private ConfidenceFactorService confidenceFactorService;

    private static PredictionLog buildPrediction(String id, String eventId, String predicted, String actual) {
        PredictionLog log = PredictionLog.builder()
                .id(id)
                .eventId(eventId)
                .sport("nba")
                .predictionType(PredictionType.MONEYLINE)
                .predictedOutcome(predicted)
                .confidence(0.65)
                .keyFactors("factors")
                .createdAt(Instant.now())
                .build();
        log.setActualOutcome(actual);
        return log;
    }

    /** Tests for {@link ConfidenceFactorService#getConfidenceFactors}. */
    @Nested
    class GetConfidenceFactors {

        @Test
        void returnsSixFactorsForEventId() {
            when(predictionLogRepository.findByEventId("evt-1")).thenReturn(List.of());
            when(predictionLogRepository.findByActualOutcomeIsNotNull()).thenReturn(List.of());

            ConfidenceFactorsResult result = confidenceFactorService.getConfidenceFactors("evt-1", null);

            assertThat(result.factors()).hasSize(6);
            assertThat(result.eventId()).isEqualTo("evt-1");
        }

        @Test
        void overallConfidenceIsWithinZeroToOneRange() {
            when(predictionLogRepository.findByEventId("evt-1")).thenReturn(List.of());
            when(predictionLogRepository.findByActualOutcomeIsNotNull()).thenReturn(List.of());

            ConfidenceFactorsResult result = confidenceFactorService.getConfidenceFactors("evt-1", null);

            assertThat(result.overallConfidence()).isBetween(0.0, 1.0);
        }

        @Test
        void resolvesPredictionIdToEventId() {
            PredictionLog pred = buildPrediction("pred-1", "evt-resolved", "WON", "WON");
            when(predictionLogRepository.findById("pred-1")).thenReturn(Optional.of(pred));
            when(predictionLogRepository.findByEventId("evt-resolved")).thenReturn(List.of(pred));
            when(predictionLogRepository.findByActualOutcomeIsNotNull()).thenReturn(List.of(pred));

            ConfidenceFactorsResult result = confidenceFactorService.getConfidenceFactors(null, "pred-1");

            assertThat(result.eventId()).isEqualTo("evt-resolved");
        }

        @Test
        void highHistoricalAccuracyIncreasesConfidence() {
            // 20 correct predictions out of 20
            List<PredictionLog> settled = List.of(
                    buildPrediction("p-1", "evt-1", "WON", "WON"),
                    buildPrediction("p-2", "evt-1", "WON", "WON"),
                    buildPrediction("p-3", "evt-1", "WON", "WON"),
                    buildPrediction("p-4", "evt-1", "WON", "WON"),
                    buildPrediction("p-5", "evt-1", "WON", "WON"),
                    buildPrediction("p-6", "evt-1", "WON", "WON"),
                    buildPrediction("p-7", "evt-1", "WON", "WON"),
                    buildPrediction("p-8", "evt-1", "WON", "WON"),
                    buildPrediction("p-9", "evt-1", "WON", "WON"),
                    buildPrediction("p-10", "evt-1", "WON", "WON"),
                    buildPrediction("p-11", "evt-1", "WON", "WON"),
                    buildPrediction("p-12", "evt-1", "WON", "WON"),
                    buildPrediction("p-13", "evt-1", "WON", "WON"),
                    buildPrediction("p-14", "evt-1", "WON", "WON"),
                    buildPrediction("p-15", "evt-1", "WON", "WON"),
                    buildPrediction("p-16", "evt-1", "WON", "WON"),
                    buildPrediction("p-17", "evt-1", "WON", "WON"),
                    buildPrediction("p-18", "evt-1", "WON", "WON"),
                    buildPrediction("p-19", "evt-1", "WON", "WON"),
                    buildPrediction("p-20", "evt-1", "WON", "WON"));
            when(predictionLogRepository.findByEventId("evt-1")).thenReturn(settled);
            when(predictionLogRepository.findByActualOutcomeIsNotNull()).thenReturn(settled);

            ConfidenceFactorsResult result = confidenceFactorService.getConfidenceFactors("evt-1", null);

            // High accuracy and sample size should push overall confidence toward high end
            assertThat(result.overallConfidence()).isGreaterThan(0.6);
        }

        @Test
        void throwsWhenNeitherEventIdNorPredictionIdProvided() {
            assertThatThrownBy(() -> confidenceFactorService.getConfidenceFactors(null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("required");
        }

        @Test
        void throwsWhenPredictionIdNotFound() {
            when(predictionLogRepository.findById("missing-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> confidenceFactorService.getConfidenceFactors(null, "missing-id"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("missing-id");
        }

        @Test
        void factorScoresAreAllWithinBounds() {
            when(predictionLogRepository.findByEventId("evt-1")).thenReturn(List.of());
            when(predictionLogRepository.findByActualOutcomeIsNotNull()).thenReturn(List.of());

            ConfidenceFactorsResult result = confidenceFactorService.getConfidenceFactors("evt-1", null);

            result.factors().forEach(f -> {
                assertThat(f.score()).isBetween(0.0, 1.0);
                assertThat(f.weight()).isGreaterThan(0.0);
                assertThat(f.factorName()).isNotBlank();
            });
        }
    }
}
