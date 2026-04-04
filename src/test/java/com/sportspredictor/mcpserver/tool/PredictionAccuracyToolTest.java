package com.sportspredictor.mcpserver.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.mcpserver.service.PredictionAccuracyService;
import com.sportspredictor.mcpserver.service.PredictionAccuracyService.AccuracyResult;
import com.sportspredictor.mcpserver.tool.PredictionAccuracyTool.AccuracyResponse;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link PredictionAccuracyTool}. */
@ExtendWith(MockitoExtension.class)
class PredictionAccuracyToolTest {

    @Mock
    private PredictionAccuracyService predictionAccuracyService;

    @InjectMocks
    private PredictionAccuracyTool predictionAccuracyTool;

    private static AccuracyResult buildAccuracyResult() {
        return new AccuracyResult(10, 7, 3, 0.7, List.of(), List.of(), List.of(), "7/10 (70.0%)");
    }

    /** Tests for {@link PredictionAccuracyTool#getPredictionAccuracy}. */
    @Nested
    class GetPredictionAccuracy {

        @Test
        void delegatesToService() {
            when(predictionAccuracyService.getAccuracy("nba", "SPREAD", "30d", 0.6))
                    .thenReturn(buildAccuracyResult());

            predictionAccuracyTool.getPredictionAccuracy("nba", "SPREAD", "30d", 0.6);

            verify(predictionAccuracyService).getAccuracy("nba", "SPREAD", "30d", 0.6);
        }

        @Test
        void returnsWrappedResult() {
            when(predictionAccuracyService.getAccuracy(null, null, null, null)).thenReturn(buildAccuracyResult());

            AccuracyResponse response = predictionAccuracyTool.getPredictionAccuracy(null, null, null, null);

            assertThat(response.totalPredictions()).isEqualTo(10);
            assertThat(response.wins()).isEqualTo(7);
            assertThat(response.losses()).isEqualTo(3);
            assertThat(response.winRate()).isEqualTo(0.7);
            assertThat(response.summary()).contains("70.0%");
        }

        @Test
        void handlesNullOptionalParams() {
            when(predictionAccuracyService.getAccuracy(null, null, null, null)).thenReturn(buildAccuracyResult());

            AccuracyResponse response = predictionAccuracyTool.getPredictionAccuracy(null, null, null, null);

            assertThat(response).isNotNull();
        }
    }
}
