package com.sportspredictor.mcpserver.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportspredictor.mcpserver.service.AdvancedAnalysisService.PaceAndTotalsResult;
import com.sportspredictor.mcpserver.service.AdvancedAnalysisService.RestAndTravelResult;
import com.sportspredictor.mcpserver.service.AdvancedAnalysisService.SituationalResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link AdvancedAnalysisService}. */
class AdvancedAnalysisServiceTest {

    private final AdvancedAnalysisService service = new AdvancedAnalysisService();

    @Nested
    class AnalyzeRestAndTravel {
        @Test
        void returnsRestAnalysis() {
            RestAndTravelResult result = service.analyzeRestAndTravel("nba", "team-1");
            assertThat(result.teamId()).isEqualTo("team-1");
            assertThat(result.restDays()).isPositive();
        }
    }

    @Nested
    class AnalyzePaceAndTotals {
        @Test
        void returnsProjectedTotal() {
            PaceAndTotalsResult result = service.analyzePaceAndTotals("nba", "team-1", "team-2");
            assertThat(result.projectedTotal()).isGreaterThan(0);
            assertThat(result.paceAdvantage()).isNotBlank();
        }
    }

    @Nested
    class AnalyzeSituationalStats {
        @Test
        void returnsSituationalBreakdown() {
            SituationalResult result = service.analyzeSituationalStats("nba", "team-1", "home");
            assertThat(result.wins()).isPositive();
            assertThat(result.coverRate()).isBetween(0.0, 1.0);
        }
    }
}
