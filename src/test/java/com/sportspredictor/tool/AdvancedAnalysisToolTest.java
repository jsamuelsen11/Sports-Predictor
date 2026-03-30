package com.sportspredictor.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sportspredictor.service.AdvancedAnalysisService;
import com.sportspredictor.service.AdvancedAnalysisService.PaceAndTotalsResult;
import com.sportspredictor.service.AdvancedAnalysisService.RestAndTravelResult;
import com.sportspredictor.service.AdvancedAnalysisService.SituationalResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link AdvancedAnalysisTool}. */
@ExtendWith(MockitoExtension.class)
class AdvancedAnalysisToolTest {

    @Mock
    private AdvancedAnalysisService advancedAnalysisService;

    @InjectMocks
    private AdvancedAnalysisTool advancedAnalysisTool;

    @Nested
    class AnalyzeRestAndTravel {
        @Test
        void delegatesToService() {
            when(advancedAnalysisService.analyzeRestAndTravel("nba", "team-1"))
                    .thenReturn(new RestAndTravelResult("team-1", 2, false, "moderate", 0, "Summary"));
            var response = advancedAnalysisTool.analyzeRestAndTravel("nba", "team-1");
            assertThat(response.teamId()).isEqualTo("team-1");
        }
    }

    @Nested
    class AnalyzePaceAndTotals {
        @Test
        void delegatesToService() {
            when(advancedAnalysisService.analyzePaceAndTotals("nba", "t1", "t2"))
                    .thenReturn(new PaceAndTotalsResult("t1", "t2", 102.0, 98.0, 100.0, "t1", "Summary"));
            var response = advancedAnalysisTool.analyzePaceAndTotals("nba", "t1", "t2");
            assertThat(response.projectedTotal()).isEqualTo(100.0);
        }
    }

    @Nested
    class AnalyzeSituationalStats {
        @Test
        void delegatesToService() {
            when(advancedAnalysisService.analyzeSituationalStats("nba", "t1", "home"))
                    .thenReturn(new SituationalResult("t1", "home", 8, 5, 0.55, 3.5, "Summary"));
            var response = advancedAnalysisTool.analyzeSituationalStats("nba", "t1", "home");
            assertThat(response.wins()).isEqualTo(8);
        }
    }
}
