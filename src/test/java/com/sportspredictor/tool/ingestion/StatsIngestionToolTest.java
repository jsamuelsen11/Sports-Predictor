package com.sportspredictor.tool.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.service.StatsService;
import com.sportspredictor.service.StatsService.HeadToHeadResult;
import com.sportspredictor.service.StatsService.MatchupGame;
import com.sportspredictor.service.StatsService.MatchupTeam;
import com.sportspredictor.service.StatsService.PlayerStatsResult;
import com.sportspredictor.service.StatsService.RecordStat;
import com.sportspredictor.service.StatsService.StatCategory;
import com.sportspredictor.service.StatsService.StatEntry;
import com.sportspredictor.service.StatsService.StatSplit;
import com.sportspredictor.service.StatsService.TeamRecordResult;
import com.sportspredictor.service.StatsService.TeamStatsResult;
import com.sportspredictor.tool.ingestion.StatsIngestionTool.HeadToHeadResponse;
import com.sportspredictor.tool.ingestion.StatsIngestionTool.PlayerStatsResponse;
import com.sportspredictor.tool.ingestion.StatsIngestionTool.TeamRecordResponse;
import com.sportspredictor.tool.ingestion.StatsIngestionTool.TeamStatsResponse;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link StatsIngestionTool}. */
@ExtendWith(MockitoExtension.class)
class StatsIngestionToolTest {

    @Mock
    private StatsService statsService;

    @InjectMocks
    private StatsIngestionTool statsIngestionTool;

    // --- Fixture builders ---

    private static StatSplit buildStatSplit(String name, int statCount) {
        List<StatEntry> stats = java.util.stream.IntStream.range(0, statCount)
                .mapToObj(i -> new StatEntry("stat" + i, i * 10.0, String.valueOf(i * 10)))
                .toList();
        StatCategory category = new StatCategory("passing", stats);
        return new StatSplit(name, List.of(category));
    }

    private static TeamStatsResult teamStatsWithSplits(String sport, String teamId, int splitCount) {
        List<StatSplit> splits = java.util.stream.IntStream.range(0, splitCount)
                .mapToObj(i -> buildStatSplit("split" + i, 3))
                .toList();
        return new TeamStatsResult(sport, teamId, splits);
    }

    private static PlayerStatsResult playerStatsWithSplits(String sport, String athleteId, int splitCount) {
        List<StatSplit> splits = java.util.stream.IntStream.range(0, splitCount)
                .mapToObj(i -> buildStatSplit("split" + i, 2))
                .toList();
        return new PlayerStatsResult(sport, athleteId, splits);
    }

    private static HeadToHeadResult buildHeadToHeadResult(String sport, String t1, String t2, int gameCount) {
        List<MatchupGame> games = java.util.stream.IntStream.range(0, gameCount)
                .mapToObj(i -> {
                    MatchupTeam team1 = new MatchupTeam(t1, "24", true);
                    MatchupTeam team2 = new MatchupTeam(t2, "17", false);
                    return new MatchupGame(
                            "g" + i,
                            "2026-01-0" + (i + 1) + "T20:00:00Z",
                            t1 + " vs " + t2,
                            true,
                            List.of(team1, team2));
                })
                .toList();
        return new HeadToHeadResult(sport, t1, t2, games, gameCount);
    }

    private static TeamRecordResult buildTeamRecord(String sport, String teamId) {
        List<RecordStat> stats = List.of(new RecordStat("wins", 12.0, "12"), new RecordStat("losses", 4.0, "4"));
        return new TeamRecordResult(sport, teamId, "Kansas City Chiefs", "AFC West", stats);
    }

    @Nested
    class GetTeamStats {

        @Test
        void delegatesToStatsService() {
            when(statsService.getTeamStats("nfl", "101", "2026")).thenReturn(teamStatsWithSplits("nfl", "101", 1));

            statsIngestionTool.getTeamStats("nfl", "101", "2026");

            verify(statsService).getTeamStats("nfl", "101", "2026");
        }

        @Test
        void buildsSummaryForEmptyResult() {
            when(statsService.getTeamStats("nfl", "101", null))
                    .thenReturn(new TeamStatsResult("nfl", "101", List.of()));

            TeamStatsResponse response = statsIngestionTool.getTeamStats("nfl", "101", null);

            assertThat(response.summary()).contains("No stats found");
            assertThat(response.summary()).contains("101");
            assertThat(response.summary()).contains("nfl");
        }

        @Test
        void buildsSummaryWithSplitAndStatCountsForNonEmptyResult() {
            when(statsService.getTeamStats("nfl", "101", null)).thenReturn(teamStatsWithSplits("nfl", "101", 2));

            TeamStatsResponse response = statsIngestionTool.getTeamStats("nfl", "101", null);

            assertThat(response.summary()).contains("101");
            assertThat(response.summary()).contains("NFL");
            assertThat(response.summary()).contains("2"); // split count
        }

        @Test
        void passesAllServiceResultFieldsThrough() {
            when(statsService.getTeamStats("nfl", "101", null)).thenReturn(teamStatsWithSplits("nfl", "101", 1));

            TeamStatsResponse response = statsIngestionTool.getTeamStats("nfl", "101", null);

            assertThat(response.sport()).isEqualTo("nfl");
            assertThat(response.teamId()).isEqualTo("101");
            assertThat(response.splits()).hasSize(1);
        }
    }

    @Nested
    class GetPlayerStats {

        @Test
        void delegatesToStatsService() {
            when(statsService.getPlayerStats("nfl", "3054211")).thenReturn(playerStatsWithSplits("nfl", "3054211", 1));

            statsIngestionTool.getPlayerStats("nfl", "3054211");

            verify(statsService).getPlayerStats("nfl", "3054211");
        }

        @Test
        void buildsSummaryForEmptyResult() {
            when(statsService.getPlayerStats("nfl", "3054211"))
                    .thenReturn(new PlayerStatsResult("nfl", "3054211", List.of()));

            PlayerStatsResponse response = statsIngestionTool.getPlayerStats("nfl", "3054211");

            assertThat(response.summary()).contains("No stats found");
            assertThat(response.summary()).contains("3054211");
            assertThat(response.summary()).contains("nfl");
        }

        @Test
        void buildsSummaryWithSplitCountForNonEmptyResult() {
            when(statsService.getPlayerStats("nba", "1234")).thenReturn(playerStatsWithSplits("nba", "1234", 3));

            PlayerStatsResponse response = statsIngestionTool.getPlayerStats("nba", "1234");

            assertThat(response.summary()).contains("1234");
            assertThat(response.summary()).contains("NBA");
            assertThat(response.summary()).contains("3");
        }

        @Test
        void passesAllServiceResultFieldsThrough() {
            when(statsService.getPlayerStats("nba", "1234")).thenReturn(playerStatsWithSplits("nba", "1234", 2));

            PlayerStatsResponse response = statsIngestionTool.getPlayerStats("nba", "1234");

            assertThat(response.sport()).isEqualTo("nba");
            assertThat(response.athleteId()).isEqualTo("1234");
            assertThat(response.splits()).hasSize(2);
        }
    }

    @Nested
    class GetHistoricalTeamStats {

        @Test
        void delegatesToStatsServiceWithResolvedSeasons() {
            when(statsService.getHistoricalTeamStats("nfl", "101", 3)).thenReturn(teamStatsWithSplits("nfl", "101", 1));

            statsIngestionTool.getHistoricalTeamStats("nfl", "101", null);

            verify(statsService).getHistoricalTeamStats("nfl", "101", 3);
        }

        @Test
        void usesProvidedNumSeasonsWhenPositive() {
            when(statsService.getHistoricalTeamStats("nfl", "101", 5)).thenReturn(teamStatsWithSplits("nfl", "101", 1));

            statsIngestionTool.getHistoricalTeamStats("nfl", "101", 5);

            verify(statsService).getHistoricalTeamStats("nfl", "101", 5);
        }

        @Test
        void usesDefaultSeasonsWhenNumSeasonsIsZero() {
            when(statsService.getHistoricalTeamStats("nfl", "101", 3)).thenReturn(teamStatsWithSplits("nfl", "101", 1));

            statsIngestionTool.getHistoricalTeamStats("nfl", "101", 0);

            verify(statsService).getHistoricalTeamStats("nfl", "101", 3);
        }

        @Test
        void buildsSummaryForEmptyResult() {
            when(statsService.getHistoricalTeamStats("nfl", "101", 3))
                    .thenReturn(new TeamStatsResult("nfl", "101", List.of()));

            TeamStatsResponse response = statsIngestionTool.getHistoricalTeamStats("nfl", "101", null);

            assertThat(response.summary()).contains("No historical stats found");
            assertThat(response.summary()).contains("101");
        }

        @Test
        void buildsSummaryWithSeasonsCountForNonEmptyResult() {
            when(statsService.getHistoricalTeamStats("nfl", "101", 3)).thenReturn(teamStatsWithSplits("nfl", "101", 1));

            TeamStatsResponse response = statsIngestionTool.getHistoricalTeamStats("nfl", "101", null);

            assertThat(response.summary()).contains("3"); // seasons requested
            assertThat(response.summary()).contains("NFL");
        }
    }

    @Nested
    class GetHistoricalPlayerStats {

        @Test
        void delegatesToStatsService() {
            when(statsService.getHistoricalPlayerStats("nfl", "3054211"))
                    .thenReturn(playerStatsWithSplits("nfl", "3054211", 2));

            statsIngestionTool.getHistoricalPlayerStats("nfl", "3054211");

            verify(statsService).getHistoricalPlayerStats("nfl", "3054211");
        }

        @Test
        void buildsSummaryForEmptyResult() {
            when(statsService.getHistoricalPlayerStats("nfl", "3054211"))
                    .thenReturn(new PlayerStatsResult("nfl", "3054211", List.of()));

            PlayerStatsResponse response = statsIngestionTool.getHistoricalPlayerStats("nfl", "3054211");

            assertThat(response.summary()).contains("No historical stats found");
            assertThat(response.summary()).contains("3054211");
        }

        @Test
        void buildsSummaryWithCareerDataNoteForNonEmptyResult() {
            when(statsService.getHistoricalPlayerStats("nba", "1234"))
                    .thenReturn(playerStatsWithSplits("nba", "1234", 2));

            PlayerStatsResponse response = statsIngestionTool.getHistoricalPlayerStats("nba", "1234");

            assertThat(response.summary()).contains("career");
            assertThat(response.summary()).contains("1234");
            assertThat(response.summary()).contains("2");
        }
    }

    @Nested
    class GetHeadToHeadHistory {

        @Test
        void delegatesToStatsServiceWithDefaultLimitWhenLastnIsNull() {
            when(statsService.getHeadToHeadHistory("nfl", "Chiefs", "Ravens", 10))
                    .thenReturn(buildHeadToHeadResult("nfl", "Chiefs", "Ravens", 0));

            statsIngestionTool.getHeadToHeadHistory("nfl", "Chiefs", "Ravens", null);

            verify(statsService).getHeadToHeadHistory("nfl", "Chiefs", "Ravens", 10);
        }

        @Test
        void usesProvidedLastnWhenPositive() {
            when(statsService.getHeadToHeadHistory("nfl", "Chiefs", "Ravens", 5))
                    .thenReturn(buildHeadToHeadResult("nfl", "Chiefs", "Ravens", 0));

            statsIngestionTool.getHeadToHeadHistory("nfl", "Chiefs", "Ravens", 5);

            verify(statsService).getHeadToHeadHistory("nfl", "Chiefs", "Ravens", 5);
        }

        @Test
        void buildsSummaryForEmptyResult() {
            when(statsService.getHeadToHeadHistory("nfl", "Chiefs", "Ravens", 10))
                    .thenReturn(new HeadToHeadResult("nfl", "Chiefs", "Ravens", List.of(), 0));

            HeadToHeadResponse response = statsIngestionTool.getHeadToHeadHistory("nfl", "Chiefs", "Ravens", null);

            assertThat(response.summary()).contains("No matchup history found");
            assertThat(response.summary()).contains("Chiefs");
            assertThat(response.summary()).contains("Ravens");
        }

        @Test
        void buildsSummaryWithGameCountForNonEmptyResult() {
            when(statsService.getHeadToHeadHistory("nfl", "Chiefs", "Ravens", 10))
                    .thenReturn(buildHeadToHeadResult("nfl", "Chiefs", "Ravens", 3));

            HeadToHeadResponse response = statsIngestionTool.getHeadToHeadHistory("nfl", "Chiefs", "Ravens", null);

            assertThat(response.summary()).contains("3");
            assertThat(response.summary()).contains("NFL");
            assertThat(response.summary()).contains("Chiefs");
            assertThat(response.summary()).contains("Ravens");
        }

        @Test
        void passesAllServiceResultFieldsThrough() {
            when(statsService.getHeadToHeadHistory("nfl", "Chiefs", "Ravens", 10))
                    .thenReturn(buildHeadToHeadResult("nfl", "Chiefs", "Ravens", 2));

            HeadToHeadResponse response = statsIngestionTool.getHeadToHeadHistory("nfl", "Chiefs", "Ravens", null);

            assertThat(response.sport()).isEqualTo("nfl");
            assertThat(response.team1()).isEqualTo("Chiefs");
            assertThat(response.team2()).isEqualTo("Ravens");
            assertThat(response.games()).hasSize(2);
            assertThat(response.count()).isEqualTo(2);
        }
    }

    @Nested
    class LookupTeamRecord {

        @Test
        void delegatesToStatsService() {
            when(statsService.getTeamRecord("nfl", "101")).thenReturn(buildTeamRecord("nfl", "101"));

            statsIngestionTool.lookupTeamRecord("nfl", "101");

            verify(statsService).getTeamRecord("nfl", "101");
        }

        @Test
        void buildsSummaryForEmptyStats() {
            when(statsService.getTeamRecord("nfl", "999"))
                    .thenReturn(new TeamRecordResult("nfl", "999", null, null, List.of()));

            TeamRecordResponse response = statsIngestionTool.lookupTeamRecord("nfl", "999");

            assertThat(response.summary()).contains("No record found");
            assertThat(response.summary()).contains("999");
        }

        @Test
        void buildsSummaryWithTeamNameAndGroupForNonEmptyResult() {
            when(statsService.getTeamRecord("nfl", "101")).thenReturn(buildTeamRecord("nfl", "101"));

            TeamRecordResponse response = statsIngestionTool.lookupTeamRecord("nfl", "101");

            assertThat(response.summary()).contains("Kansas City Chiefs");
            assertThat(response.summary()).contains("NFL");
            assertThat(response.summary()).contains("AFC West");
            assertThat(response.summary()).contains("2"); // stat count
        }

        @Test
        void summaryUsesTeamIdWhenTeamNameIsNull() {
            TeamRecordResult noName =
                    new TeamRecordResult("nfl", "999", null, "AFC East", List.of(new RecordStat("wins", 5.0, "5")));
            when(statsService.getTeamRecord("nfl", "999")).thenReturn(noName);

            TeamRecordResponse response = statsIngestionTool.lookupTeamRecord("nfl", "999");

            assertThat(response.summary()).contains("999");
        }

        @Test
        void passesAllServiceResultFieldsThrough() {
            when(statsService.getTeamRecord("nfl", "101")).thenReturn(buildTeamRecord("nfl", "101"));

            TeamRecordResponse response = statsIngestionTool.lookupTeamRecord("nfl", "101");

            assertThat(response.sport()).isEqualTo("nfl");
            assertThat(response.teamId()).isEqualTo("101");
            assertThat(response.teamName()).isEqualTo("Kansas City Chiefs");
            assertThat(response.group()).isEqualTo("AFC West");
            assertThat(response.stats()).hasSize(2);
        }
    }
}
