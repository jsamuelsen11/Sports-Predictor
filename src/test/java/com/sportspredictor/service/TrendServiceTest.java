package com.sportspredictor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sportspredictor.service.ResultsService.GameResult;
import com.sportspredictor.service.ResultsService.GameResultsResult;
import com.sportspredictor.service.ResultsService.TeamResult;
import com.sportspredictor.service.StatsService.StatCategory;
import com.sportspredictor.service.StatsService.StatEntry;
import com.sportspredictor.service.StatsService.StatSplit;
import com.sportspredictor.service.StatsService.TeamStatsResult;
import com.sportspredictor.service.TrendService.TrendResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link TrendService}. */
@ExtendWith(MockitoExtension.class)
class TrendServiceTest {

    @Mock
    private StatsService statsService;

    @Mock
    private ResultsService resultsService;

    @InjectMocks
    private TrendService trendService;

    private static TeamStatsResult buildTeamStats(String teamId) {
        var stat1 = new StatEntry("points", 105.5, "105.5");
        var stat2 = new StatEntry("rebounds", 44.2, "44.2");
        var category = new StatCategory("general", List.of(stat1, stat2));
        var split = new StatSplit("Overall", List.of(category));
        return new TeamStatsResult("nba", teamId, List.of(split));
    }

    private static GameResultsResult buildResults(String teamId, int wins, int losses) {
        List<GameResult> results = new ArrayList<>();
        for (int i = 0; i < wins; i++) {
            var team = new TeamResult(teamId, "Team A", "TA", "110", true);
            var opp = new TeamResult("opp", "Team B", "TB", "100", false);
            results.add(new GameResult("g" + i, "2026-01-0" + (i + 1), "Game " + i, List.of(team, opp)));
        }
        for (int i = 0; i < losses; i++) {
            var team = new TeamResult(teamId, "Team A", "TA", "90", false);
            var opp = new TeamResult("opp", "Team B", "TB", "100", true);
            results.add(new GameResult("l" + i, "2026-01-1" + i, "Game L" + i, List.of(team, opp)));
        }
        return new GameResultsResult("nba", null, results, results.size());
    }

    /** Tests for trend metric computation. */
    @Nested
    class TrendMetrics {

        @Test
        void returnsMetricsFromStats() {
            when(statsService.getTeamStats("nba", "1", null)).thenReturn(buildTeamStats("1"));
            when(resultsService.getGameResults(eq("nba"), any(), any())).thenReturn(buildResults("1", 5, 5));

            TrendResult result = trendService.analyzeTrends("nba", "1", "last_10", null);

            assertThat(result.metrics()).hasSize(2);
        }

        @Test
        void filtersMetricsByName() {
            when(statsService.getTeamStats("nba", "1", null)).thenReturn(buildTeamStats("1"));
            when(resultsService.getGameResults(eq("nba"), any(), any())).thenReturn(buildResults("1", 5, 5));

            TrendResult result = trendService.analyzeTrends("nba", "1", "last_10", List.of("points"));

            assertThat(result.metrics()).hasSize(1);
            assertThat(result.metrics().get(0).name()).isEqualTo("points");
        }
    }

    /** Tests for win-loss record computation. */
    @Nested
    class RecordComputation {

        @Test
        void computesWinLossRecord() {
            when(statsService.getTeamStats("nba", "1", null)).thenReturn(buildTeamStats("1"));
            when(resultsService.getGameResults(eq("nba"), any(), any())).thenReturn(buildResults("1", 7, 3));

            TrendResult result = trendService.analyzeTrends("nba", "1", "last_10", null);

            assertThat(result.record().wins()).isEqualTo(7);
            assertThat(result.record().losses()).isEqualTo(3);
            assertThat(result.record().winRate()).isCloseTo(0.7, within(0.001));
        }

        @Test
        void emptyResultsYieldsZeroRecord() {
            when(statsService.getTeamStats("nba", "1", null)).thenReturn(buildTeamStats("1"));
            when(resultsService.getGameResults(eq("nba"), any(), any()))
                    .thenReturn(new GameResultsResult("nba", null, List.of(), 0));

            TrendResult result = trendService.analyzeTrends("nba", "1", "last_10", null);

            assertThat(result.record().wins()).isEqualTo(0);
            assertThat(result.record().losses()).isEqualTo(0);
            assertThat(result.record().winRate()).isEqualTo(0.0);
        }
    }

    /** Tests for window parsing. */
    @Nested
    class WindowParsing {

        @Test
        void parsesLast5() {
            assertThat(TrendService.parseWindowSize("last_5")).isEqualTo(5);
        }

        @Test
        void parsesLast10() {
            assertThat(TrendService.parseWindowSize("last_10")).isEqualTo(10);
        }

        @Test
        void parsesLast20() {
            assertThat(TrendService.parseWindowSize("last_20")).isEqualTo(20);
        }

        @Test
        void parsesSeason() {
            assertThat(TrendService.parseWindowSize("season")).isEqualTo(999);
        }

        @Test
        void defaultsToTen() {
            assertThat(TrendService.parseWindowSize(null)).isEqualTo(10);
        }
    }

    /** Tests for default behavior. */
    @Nested
    class DefaultBehavior {

        @Test
        void defaultWindowIsLast10() {
            when(statsService.getTeamStats("nba", "1", null)).thenReturn(buildTeamStats("1"));
            when(resultsService.getGameResults(eq("nba"), any(), any())).thenReturn(buildResults("1", 5, 5));

            TrendResult result = trendService.analyzeTrends("nba", "1", null, null);

            assertThat(result.window()).isEqualTo("last_10");
        }

        @Test
        void emptyStatsProducesEmptyMetrics() {
            var emptyStats = new TeamStatsResult("nba", "1", List.of());
            when(statsService.getTeamStats("nba", "1", null)).thenReturn(emptyStats);
            when(resultsService.getGameResults(eq("nba"), any(), any())).thenReturn(buildResults("1", 0, 0));

            TrendResult result = trendService.analyzeTrends("nba", "1", "last_10", null);

            assertThat(result.metrics()).isEmpty();
        }
    }

    /** Tests for summary content. */
    @Nested
    class SummaryContent {

        @Test
        void summaryContainsTeamInfo() {
            when(statsService.getTeamStats("nba", "1", null)).thenReturn(buildTeamStats("1"));
            when(resultsService.getGameResults(eq("nba"), any(), any())).thenReturn(buildResults("1", 5, 5));

            TrendResult result = trendService.analyzeTrends("nba", "1", "last_10", null);

            assertThat(result.summary()).contains("1");
            assertThat(result.summary()).contains("NBA");
        }
    }
}
