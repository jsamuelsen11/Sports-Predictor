package com.sportspredictor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sportspredictor.service.MonteCarloService.SimulationOutput;
import com.sportspredictor.service.StatsService.StatCategory;
import com.sportspredictor.service.StatsService.StatEntry;
import com.sportspredictor.service.StatsService.StatSplit;
import com.sportspredictor.service.StatsService.TeamStatsResult;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link MonteCarloService}. */
@ExtendWith(MockitoExtension.class)
class MonteCarloServiceTest {

    @Mock
    private StatsService statsService;

    @InjectMocks
    private MonteCarloService monteCarloService;

    private static TeamStatsResult buildTeamStats(String sport, String teamId, double ppg) {
        return new TeamStatsResult(
                sport,
                teamId,
                List.of(new StatSplit(
                        "Overall",
                        List.of(new StatCategory(
                                "scoring", List.of(new StatEntry("pointsPerGame", ppg, String.valueOf(ppg))))))));
    }

    /** Tests for {@link MonteCarloService#simulateGame}. */
    @Nested
    class SimulateGame {

        @Test
        void winAndDrawProbabilitiesSumToOne() {
            when(statsService.getTeamStats(eq("nba"), eq("lakers"), any()))
                    .thenReturn(buildTeamStats("nba", "lakers", 112.5));
            when(statsService.getTeamStats(eq("nba"), eq("celtics"), any()))
                    .thenReturn(buildTeamStats("nba", "celtics", 108.0));

            SimulationOutput output = monteCarloService.simulateGame("nba", "lakers", "celtics", 5_000);

            double total = output.team1WinProbability() + output.team2WinProbability() + output.drawProbability();
            assertThat(total).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        void spreadDistributionMeanReflectsTeamDifference() {
            when(statsService.getTeamStats(eq("nba"), eq("lakers"), any()))
                    .thenReturn(buildTeamStats("nba", "lakers", 115.0));
            when(statsService.getTeamStats(eq("nba"), eq("clippers"), any()))
                    .thenReturn(buildTeamStats("nba", "clippers", 105.0));

            SimulationOutput output = monteCarloService.simulateGame("nba", "lakers", "clippers", 10_000);

            // With team1 scoring ~10 more PPG than team2 on average, mean spread should be positive
            assertThat(output.spreadDistribution().meanSpread()).isGreaterThan(0.0);
        }

        @Test
        void defaultSimulationCountUsedWhenNull() {
            when(statsService.getTeamStats(eq("nfl"), eq("chiefs"), any()))
                    .thenReturn(buildTeamStats("nfl", "chiefs", 27.5));
            when(statsService.getTeamStats(eq("nfl"), eq("bills"), any()))
                    .thenReturn(buildTeamStats("nfl", "bills", 25.0));

            SimulationOutput output = monteCarloService.simulateGame("nfl", "chiefs", "bills", null);

            assertThat(output.numSimulations()).isEqualTo(10_000);
        }

        @Test
        void defaultSimulationCountUsedWhenZeroOrNegative() {
            when(statsService.getTeamStats(eq("nba"), eq("lakers"), any()))
                    .thenReturn(buildTeamStats("nba", "lakers", 112.5));
            when(statsService.getTeamStats(eq("nba"), eq("celtics"), any()))
                    .thenReturn(buildTeamStats("nba", "celtics", 108.0));

            SimulationOutput outputZero = monteCarloService.simulateGame("nba", "lakers", "celtics", 0);
            SimulationOutput outputNeg = monteCarloService.simulateGame("nba", "lakers", "celtics", -100);

            assertThat(outputZero.numSimulations()).isEqualTo(10_000);
            assertThat(outputNeg.numSimulations()).isEqualTo(10_000);
        }

        @Test
        void simulationCountCappedAtMaximum() {
            when(statsService.getTeamStats(eq("nba"), eq("lakers"), any()))
                    .thenReturn(buildTeamStats("nba", "lakers", 112.5));
            when(statsService.getTeamStats(eq("nba"), eq("celtics"), any()))
                    .thenReturn(buildTeamStats("nba", "celtics", 108.0));

            SimulationOutput output = monteCarloService.simulateGame("nba", "lakers", "celtics", 999_999);

            assertThat(output.numSimulations()).isEqualTo(100_000);
        }

        @Test
        void fallsBackToDefaultsWhenStatsServiceThrows() {
            when(statsService.getTeamStats(any(), any(), any())).thenThrow(new RuntimeException("API unavailable"));

            SimulationOutput output = monteCarloService.simulateGame("nba", "unknown-team1", "unknown-team2", 1_000);

            // Should still produce a valid output using defaults
            double total = output.team1WinProbability() + output.team2WinProbability() + output.drawProbability();
            assertThat(total).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001));
            assertThat(output.numSimulations()).isEqualTo(1_000);
        }

        @Test
        void outputFieldsArePopulated() {
            when(statsService.getTeamStats(eq("nba"), eq("lakers"), any()))
                    .thenReturn(buildTeamStats("nba", "lakers", 112.5));
            when(statsService.getTeamStats(eq("nba"), eq("celtics"), any()))
                    .thenReturn(buildTeamStats("nba", "celtics", 108.0));

            SimulationOutput output = monteCarloService.simulateGame("nba", "lakers", "celtics", 2_000);

            assertThat(output.sport()).isEqualTo("nba");
            assertThat(output.team1Id()).isEqualTo("lakers");
            assertThat(output.team2Id()).isEqualTo("celtics");
            assertThat(output.summary()).isNotBlank();
            assertThat(output.team1Scores()).isNotNull();
            assertThat(output.team2Scores()).isNotNull();
            assertThat(output.totalDistribution().overProbability()
                            + output.totalDistribution().underProbability())
                    .isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001));
        }
    }
}
