package com.sportspredictor.mcpserver.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.mcpserver.service.StatsService;
import com.sportspredictor.mcpserver.tool.MatchupTool.MatchupResponse;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link MatchupTool}. */
@ExtendWith(MockitoExtension.class)
class MatchupToolTest {

    @Mock
    private StatsService statsService;

    @InjectMocks
    private MatchupTool matchupTool;

    private static StatsService.TeamStatsResult buildTeamStats(String teamId, double points, double rebounds) {
        var pointsStat = new StatsService.StatEntry("points", points, String.valueOf(points));
        var reboundsStat = new StatsService.StatEntry("rebounds", rebounds, String.valueOf(rebounds));
        var category = new StatsService.StatCategory("general", List.of(pointsStat, reboundsStat));
        var split = new StatsService.StatSplit("Overall", List.of(category));
        return new StatsService.TeamStatsResult("nba", teamId, List.of(split));
    }

    private static StatsService.HeadToHeadResult buildHeadToHead(
            String team1, String team2, int team1Wins, int team2Wins) {
        List<StatsService.MatchupGame> games = new java.util.ArrayList<>();
        for (int i = 0; i < team1Wins; i++) {
            var t1 = new StatsService.MatchupTeam(team1, "110", true);
            var t2 = new StatsService.MatchupTeam(team2, "100", false);
            games.add(new StatsService.MatchupGame("g" + i, "2026-01-0" + (i + 1), "Game " + i, true, List.of(t1, t2)));
        }
        for (int i = 0; i < team2Wins; i++) {
            var t1 = new StatsService.MatchupTeam(team1, "90", false);
            var t2 = new StatsService.MatchupTeam(team2, "100", true);
            games.add(
                    new StatsService.MatchupGame("l" + i, "2026-02-0" + (i + 1), "Game L" + i, true, List.of(t1, t2)));
        }
        return new StatsService.HeadToHeadResult("nba", team1, team2, games, games.size());
    }

    /** Tests for {@link MatchupTool#compareMatchup}. */
    @Nested
    class CompareMatchup {

        @Test
        void returnsComparisonWithStats() {
            when(statsService.getTeamStats("nba", "1", null)).thenReturn(buildTeamStats("1", 110.0, 45.0));
            when(statsService.getTeamStats("nba", "2", null)).thenReturn(buildTeamStats("2", 105.0, 48.0));
            when(statsService.getHeadToHeadHistory("nba", "1", "2", null)).thenReturn(buildHeadToHead("1", "2", 0, 0));

            MatchupResponse result = matchupTool.compareMatchup("nba", "1", "2", null);

            assertThat(result.comparisons()).hasSize(2);
            assertThat(result.comparisons().get(0).statName()).isEqualTo("points");
            assertThat(result.comparisons().get(0).advantage()).isEqualTo("TEAM1");
            assertThat(result.comparisons().get(1).statName()).isEqualTo("rebounds");
            assertThat(result.comparisons().get(1).advantage()).isEqualTo("TEAM2");
        }

        @Test
        void handlesEmptyStats() {
            var empty1 = new StatsService.TeamStatsResult("nba", "1", List.of());
            var empty2 = new StatsService.TeamStatsResult("nba", "2", List.of());
            when(statsService.getTeamStats("nba", "1", null)).thenReturn(empty1);
            when(statsService.getTeamStats("nba", "2", null)).thenReturn(empty2);
            when(statsService.getHeadToHeadHistory("nba", "1", "2", null)).thenReturn(buildHeadToHead("1", "2", 0, 0));

            MatchupResponse result = matchupTool.compareMatchup("nba", "1", "2", null);

            assertThat(result.comparisons()).isEmpty();
            assertThat(result.summary()).isNotBlank();
        }

        @Test
        void filtersStatsByName() {
            when(statsService.getTeamStats("nba", "1", null)).thenReturn(buildTeamStats("1", 110.0, 45.0));
            when(statsService.getTeamStats("nba", "2", null)).thenReturn(buildTeamStats("2", 105.0, 48.0));
            when(statsService.getHeadToHeadHistory("nba", "1", "2", null)).thenReturn(buildHeadToHead("1", "2", 0, 0));

            MatchupResponse result = matchupTool.compareMatchup("nba", "1", "2", "points");

            assertThat(result.comparisons()).hasSize(1);
            assertThat(result.comparisons().get(0).statName()).isEqualTo("points");
        }

        @Test
        void headToHeadSummary() {
            when(statsService.getTeamStats("nba", "1", null)).thenReturn(buildTeamStats("1", 100.0, 40.0));
            when(statsService.getTeamStats("nba", "2", null)).thenReturn(buildTeamStats("2", 100.0, 40.0));
            when(statsService.getHeadToHeadHistory("nba", "1", "2", null)).thenReturn(buildHeadToHead("1", "2", 2, 1));

            MatchupResponse result = matchupTool.compareMatchup("nba", "1", "2", null);

            assertThat(result.headToHead().totalGames()).isEqualTo(3);
            assertThat(result.headToHead().team1Wins()).isEqualTo(2);
            assertThat(result.headToHead().team2Wins()).isEqualTo(1);
        }

        @Test
        void emptyHeadToHead() {
            when(statsService.getTeamStats("nba", "1", null)).thenReturn(buildTeamStats("1", 100.0, 40.0));
            when(statsService.getTeamStats("nba", "2", null)).thenReturn(buildTeamStats("2", 100.0, 40.0));
            when(statsService.getHeadToHeadHistory("nba", "1", "2", null))
                    .thenReturn(new StatsService.HeadToHeadResult("nba", "1", "2", List.of(), 0));

            MatchupResponse result = matchupTool.compareMatchup("nba", "1", "2", null);

            assertThat(result.headToHead().totalGames()).isEqualTo(0);
            assertThat(result.headToHead().team1Wins()).isEqualTo(0);
            assertThat(result.headToHead().team2Wins()).isEqualTo(0);
        }

        @Test
        void delegatesToService() {
            when(statsService.getTeamStats("nba", "1", null)).thenReturn(buildTeamStats("1", 100.0, 40.0));
            when(statsService.getTeamStats("nba", "2", null)).thenReturn(buildTeamStats("2", 100.0, 40.0));
            when(statsService.getHeadToHeadHistory("nba", "1", "2", null)).thenReturn(buildHeadToHead("1", "2", 0, 0));

            matchupTool.compareMatchup("nba", "1", "2", null);

            verify(statsService).getTeamStats("nba", "1", null);
            verify(statsService).getTeamStats("nba", "2", null);
            verify(statsService).getHeadToHeadHistory("nba", "1", "2", null);
        }

        @Test
        void summaryContainsTeamIds() {
            when(statsService.getTeamStats("nba", "1", null)).thenReturn(buildTeamStats("1", 100.0, 40.0));
            when(statsService.getTeamStats("nba", "2", null)).thenReturn(buildTeamStats("2", 100.0, 40.0));
            when(statsService.getHeadToHeadHistory("nba", "1", "2", null)).thenReturn(buildHeadToHead("1", "2", 0, 0));

            MatchupResponse result = matchupTool.compareMatchup("nba", "1", "2", null);

            assertThat(result.summary()).contains("1");
            assertThat(result.summary()).contains("2");
        }

        @Test
        void evenStatsShowEvenAdvantage() {
            when(statsService.getTeamStats("nba", "1", null)).thenReturn(buildTeamStats("1", 100.0, 40.0));
            when(statsService.getTeamStats("nba", "2", null)).thenReturn(buildTeamStats("2", 100.0, 40.0));
            when(statsService.getHeadToHeadHistory("nba", "1", "2", null)).thenReturn(buildHeadToHead("1", "2", 0, 0));

            MatchupResponse result = matchupTool.compareMatchup("nba", "1", "2", null);

            assertThat(result.comparisons())
                    .allSatisfy(c -> assertThat(c.advantage()).isEqualTo("EVEN"));
        }
    }
}
