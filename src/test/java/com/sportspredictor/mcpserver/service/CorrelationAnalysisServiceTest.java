package com.sportspredictor.mcpserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sportspredictor.mcpserver.service.CorrelationAnalysisService.CorrelationResult;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link CorrelationAnalysisService}. */
@ExtendWith(MockitoExtension.class)
class CorrelationAnalysisServiceTest {

    @InjectMocks
    private CorrelationAnalysisService correlationAnalysisService;

    /** Tests for {@link CorrelationAnalysisService#analyzeCorrelations}. */
    @Nested
    class AnalyzeCorrelations {

        @Test
        void returnsTwoCorrelationsForTwoLegs() {
            List<String> legs = List.of("Lakers ML", "Celtics ML");
            List<Integer> odds = List.of(-150, +110);

            CorrelationResult result = correlationAnalysisService.analyzeCorrelations("nba", "evt-1", legs, odds);

            // For 2 legs: C(2,2) = 1 pair
            assertThat(result.correlations()).hasSize(1);
            assertThat(result.correlationMatrix()).hasSize(2);
        }

        @Test
        void independentLegsHaveZeroCorrelation() {
            // Two moneylines from different teams — no shared player keywords
            List<String> legs = List.of("Warriors ML", "Heat ML");
            List<Integer> odds = List.of(-130, -120);

            CorrelationResult result = correlationAnalysisService.analyzeCorrelations("nba", "evt-1", legs, odds);

            assertThat(result.correlations().getFirst().correlationCoefficient())
                    .isEqualTo(0.0);
        }

        @Test
        void teamWinAndPlayerPointsShowsModeratePositiveCorrelation() {
            // Moneyline + player points — correlated because team scoring drives both
            List<String> legs = List.of("lakers to win", "lebron points over 25");
            List<Integer> odds = List.of(-150, -110);

            CorrelationResult result = correlationAnalysisService.analyzeCorrelations("nba", "evt-1", legs, odds);

            assertThat(result.correlations().getFirst().correlationCoefficient())
                    .isGreaterThan(0.0);
        }

        @Test
        void samePlayerPropsShowStrongPositiveCorrelation() {
            List<String> legs = List.of("lebron points over 25", "lebron rebounds over 7");
            List<Integer> odds = List.of(-110, -110);

            CorrelationResult result = correlationAnalysisService.analyzeCorrelations("nba", "evt-1", legs, odds);

            assertThat(result.correlations().getFirst().correlationCoefficient())
                    .isGreaterThanOrEqualTo(0.6);
        }

        @Test
        void adjustedProbabilityIsHigherThanUnadjustedForPositiveCorrelation() {
            List<String> legs = List.of("lakers to win", "lebron points over 25");
            List<Integer> odds = List.of(-150, -110);

            CorrelationResult result = correlationAnalysisService.analyzeCorrelations("nba", "evt-1", legs, odds);

            // Positive correlation should boost the combined probability above the independent product
            assertThat(result.adjustedCombinedProbability())
                    .isGreaterThanOrEqualTo(result.unadjustedCombinedProbability());
        }

        @Test
        void throwsWhenFewerThanTwoLegsProvided() {
            List<String> legs = List.of("Lakers ML");
            List<Integer> odds = List.of(-150);

            assertThatThrownBy(() -> correlationAnalysisService.analyzeCorrelations("nba", "evt-1", legs, odds))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 2");
        }

        @Test
        void throwsWhenNullLegsProvided() {
            assertThatThrownBy(() -> correlationAnalysisService.analyzeCorrelations("nba", "evt-1", null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void throwsWhenOddsSizeMismatchesLegs() {
            List<String> legs = List.of("Lakers ML", "Celtics ML");
            List<Integer> odds = List.of(-150); // only one odd for two legs

            assertThatThrownBy(() -> correlationAnalysisService.analyzeCorrelations("nba", "evt-1", legs, odds))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("same length");
        }

        @Test
        void summaryContainsLegCount() {
            List<String> legs = List.of("Lakers ML", "Celtics ML", "Heat ML");
            List<Integer> odds = List.of(-150, +110, -105);

            CorrelationResult result = correlationAnalysisService.analyzeCorrelations("nba", "evt-1", legs, odds);

            assertThat(result.summary()).contains("3 legs");
            // 3 legs produce C(3,2) = 3 pairs
            assertThat(result.correlations()).hasSize(3);
        }
    }
}
