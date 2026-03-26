package com.sportspredictor.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.service.PredictionService;
import com.sportspredictor.service.PredictionService.PredictionResult;
import com.sportspredictor.tool.PredictionTool.PredictionResponse;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link PredictionTool}. */
@ExtendWith(MockitoExtension.class)
class PredictionToolTest {

    @Mock
    private PredictionService predictionService;

    @InjectMocks
    private PredictionTool predictionTool;

    private static PredictionResult buildPredictionResult() {
        return new PredictionResult(
                "pred-1",
                "evt-1",
                "nba",
                "1",
                "2",
                "MONEYLINE",
                "1",
                0.65,
                0.65,
                List.of("Stats advantage"),
                null,
                null,
                null,
                null,
                null,
                "Prediction summary");
    }

    /** Tests for {@link PredictionTool#generatePrediction}. */
    @Nested
    class GeneratePrediction {

        @Test
        void delegatesToService() {
            when(predictionService.generatePrediction("nba", "evt-1", "1", "2", "MONEYLINE", null, null, null))
                    .thenReturn(buildPredictionResult());

            predictionTool.generatePrediction("nba", "evt-1", "1", "2", "MONEYLINE", null, null, null);

            verify(predictionService).generatePrediction("nba", "evt-1", "1", "2", "MONEYLINE", null, null, null);
        }

        @Test
        void wrapsServiceResult() {
            when(predictionService.generatePrediction("nba", "evt-1", "1", "2", "MONEYLINE", null, null, null))
                    .thenReturn(buildPredictionResult());

            PredictionResponse response =
                    predictionTool.generatePrediction("nba", "evt-1", "1", "2", "MONEYLINE", null, null, null);

            assertThat(response.predictionId()).isEqualTo("pred-1");
            assertThat(response.predictedOutcome()).isEqualTo("1");
            assertThat(response.confidence()).isEqualTo(0.65);
            assertThat(response.summary()).isEqualTo("Prediction summary");
        }

        @Test
        void passesOptionalWeatherParams() {
            when(predictionService.generatePrediction(
                            "nba", "evt-1", "1", "2", "MONEYLINE", 34.0, -118.0, "2026-01-15"))
                    .thenReturn(buildPredictionResult());

            predictionTool.generatePrediction("nba", "evt-1", "1", "2", "MONEYLINE", 34.0, -118.0, "2026-01-15");

            verify(predictionService)
                    .generatePrediction("nba", "evt-1", "1", "2", "MONEYLINE", 34.0, -118.0, "2026-01-15");
        }
    }
}
