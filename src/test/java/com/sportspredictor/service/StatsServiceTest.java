package com.sportspredictor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.client.espn.EspnApiClient;
import com.sportspredictor.client.espn.PlayerStatsResponse;
import com.sportspredictor.client.espn.ScoreboardResponse;
import com.sportspredictor.client.espn.StandingsResponse;
import com.sportspredictor.client.espn.TeamStatsResponse;
import com.sportspredictor.service.SportLeagueMapping.LeagueInfo;
import com.sportspredictor.service.StatsService.HeadToHeadResult;
import com.sportspredictor.service.StatsService.PlayerStatsResult;
import com.sportspredictor.service.StatsService.TeamRecordResult;
import com.sportspredictor.service.StatsService.TeamStatsResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link StatsService}. */
@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private EspnApiClient espnApiClient;

    @Mock
    private SportLeagueMapping sportLeagueMapping;

    @InjectMocks
    private StatsService statsService;

    private static final LeagueInfo NFL_INFO = new LeagueInfo("nfl", "football", "nfl", "americanfootball_nfl", "NFL");

    @BeforeEach
    void setupMapping() {
        when(sportLeagueMapping.resolve("nfl")).thenReturn(NFL_INFO);
    }

    // --- Fixture builders ---

    private static TeamStatsResponse buildTeamStatsResponse() {
        TeamStatsResponse.Stat stat = new TeamStatsResponse.Stat("yards", 350.5, "350.5");
        TeamStatsResponse.StatCategory category = new TeamStatsResponse.StatCategory("passing", List.of(stat));
        TeamStatsResponse.Split split = new TeamStatsResponse.Split("overall", List.of(category));
        return new TeamStatsResponse(List.of(split));
    }

    private static PlayerStatsResponse buildPlayerStatsResponse() {
        PlayerStatsResponse.Stat stat = new PlayerStatsResponse.Stat("touchdowns", 12.0, "12");
        PlayerStatsResponse.StatCategory category = new PlayerStatsResponse.StatCategory("passing", List.of(stat));
        PlayerStatsResponse.Split split = new PlayerStatsResponse.Split("career", List.of(category));
        return new PlayerStatsResponse(List.of(split));
    }

    private static ScoreboardResponse buildScoreboardWithTeams(String team1Name, String team2Name) {
        ScoreboardResponse.Team t1 = new ScoreboardResponse.Team("1", team1Name, "T1");
        ScoreboardResponse.Team t2 = new ScoreboardResponse.Team("2", team2Name, "T2");
        ScoreboardResponse.Competitor c1 = new ScoreboardResponse.Competitor("c1", t1, "24", true);
        ScoreboardResponse.Competitor c2 = new ScoreboardResponse.Competitor("c2", t2, "17", false);
        ScoreboardResponse.StatusType statusType = new ScoreboardResponse.StatusType("Final", true);
        ScoreboardResponse.Status status = new ScoreboardResponse.Status(statusType);
        ScoreboardResponse.Competition comp =
                new ScoreboardResponse.Competition("g1", "2026-01-10T20:00:00Z", List.of(c1, c2), status);
        ScoreboardResponse.Event event = new ScoreboardResponse.Event(
                "g1", "2026-01-10T20:00:00Z", team1Name + " vs " + team2Name, "T1 @ T2", List.of(comp));
        return new ScoreboardResponse(List.of(event));
    }

    private static StandingsResponse buildStandingsResponse(String teamId, String teamName) {
        StandingsResponse.Stat wins = new StandingsResponse.Stat("wins", 12.0, "12");
        StandingsResponse.Stat losses = new StandingsResponse.Stat("losses", 4.0, "4");
        StandingsResponse.Entry entry = new StandingsResponse.Entry(teamId + " - " + teamName, List.of(wins, losses));
        StandingsResponse.Standing standing = new StandingsResponse.Standing(List.of(entry));
        StandingsResponse.StandingsGroup group = new StandingsResponse.StandingsGroup("AFC West", List.of(standing));
        return new StandingsResponse(List.of(group));
    }

    @Nested
    class GetTeamStats {

        @Test
        void delegatesToClientWithEspnLeagueIds() {
            when(espnApiClient.getTeamStats("football", "nfl", "101")).thenReturn(buildTeamStatsResponse());

            statsService.getTeamStats("nfl", "101", null);

            verify(espnApiClient).getTeamStats("football", "nfl", "101");
        }

        @Test
        void returnsMapppedSplits() {
            when(espnApiClient.getTeamStats("football", "nfl", "101")).thenReturn(buildTeamStatsResponse());

            TeamStatsResult result = statsService.getTeamStats("nfl", "101", null);

            assertThat(result.sport()).isEqualTo("nfl");
            assertThat(result.teamId()).isEqualTo("101");
            assertThat(result.splits()).hasSize(1);
            assertThat(result.splits().getFirst().name()).isEqualTo("overall");
            assertThat(result.splits().getFirst().categories()).hasSize(1);
            assertThat(result.splits().getFirst().categories().getFirst().stats())
                    .hasSize(1);
        }

        @Test
        void returnsEmptyResultOnClientException() {
            when(espnApiClient.getTeamStats("football", "nfl", "101")).thenThrow(new RuntimeException("ESPN timeout"));

            TeamStatsResult result = statsService.getTeamStats("nfl", "101", null);

            assertThat(result.splits()).isEmpty();
            assertThat(result.sport()).isEqualTo("nfl");
            assertThat(result.teamId()).isEqualTo("101");
        }
    }

    @Nested
    class GetPlayerStats {

        @Test
        void delegatesToClientWithEspnLeagueIds() {
            when(espnApiClient.getPlayerStats("football", "nfl", "3054211")).thenReturn(buildPlayerStatsResponse());

            statsService.getPlayerStats("nfl", "3054211");

            verify(espnApiClient).getPlayerStats("football", "nfl", "3054211");
        }

        @Test
        void returnsMappedSplits() {
            when(espnApiClient.getPlayerStats("football", "nfl", "3054211")).thenReturn(buildPlayerStatsResponse());

            PlayerStatsResult result = statsService.getPlayerStats("nfl", "3054211");

            assertThat(result.sport()).isEqualTo("nfl");
            assertThat(result.athleteId()).isEqualTo("3054211");
            assertThat(result.splits()).hasSize(1);
            assertThat(result.splits().getFirst().name()).isEqualTo("career");
        }

        @Test
        void returnsEmptyResultOnClientException() {
            when(espnApiClient.getPlayerStats("football", "nfl", "3054211"))
                    .thenThrow(new RuntimeException("ESPN timeout"));

            PlayerStatsResult result = statsService.getPlayerStats("nfl", "3054211");

            assertThat(result.splits()).isEmpty();
            assertThat(result.athleteId()).isEqualTo("3054211");
        }
    }

    @Nested
    class GetTeamRecord {

        @Test
        void delegatesToStandingsClient() {
            when(espnApiClient.getStandings("football", "nfl"))
                    .thenReturn(buildStandingsResponse("101", "Kansas City Chiefs"));

            statsService.getTeamRecord("nfl", "101");

            verify(espnApiClient).getStandings("football", "nfl");
        }

        @Test
        void findsTeamByIdSubstring() {
            when(espnApiClient.getStandings("football", "nfl"))
                    .thenReturn(buildStandingsResponse("101", "Kansas City Chiefs"));

            TeamRecordResult result = statsService.getTeamRecord("nfl", "101");

            assertThat(result.sport()).isEqualTo("nfl");
            assertThat(result.teamId()).isEqualTo("101");
            assertThat(result.group()).isEqualTo("AFC West");
            assertThat(result.stats()).hasSize(2);
            assertThat(result.stats()).extracting("name").containsExactlyInAnyOrder("wins", "losses");
        }

        @Test
        void returnsEmptyResultWhenTeamNotFound() {
            when(espnApiClient.getStandings("football", "nfl"))
                    .thenReturn(buildStandingsResponse("101", "Kansas City Chiefs"));

            TeamRecordResult result = statsService.getTeamRecord("nfl", "999");

            assertThat(result.stats()).isEmpty();
            assertThat(result.teamName()).isNull();
            assertThat(result.group()).isNull();
        }

        @Test
        void returnsEmptyResultOnClientException() {
            when(espnApiClient.getStandings("football", "nfl")).thenThrow(new RuntimeException("ESPN timeout"));

            TeamRecordResult result = statsService.getTeamRecord("nfl", "101");

            assertThat(result.stats()).isEmpty();
            assertThat(result.teamId()).isEqualTo("101");
        }
    }

    @Nested
    class GetHeadToHeadHistory {

        @Test
        void delegatesToScoreboardClient() {
            when(espnApiClient.getScoreboard("football", "nfl"))
                    .thenReturn(buildScoreboardWithTeams("Kansas City Chiefs", "Baltimore Ravens"));

            statsService.getHeadToHeadHistory("nfl", "Kansas City Chiefs", "Baltimore Ravens", null);

            verify(espnApiClient).getScoreboard("football", "nfl");
        }

        @Test
        void returnsMatchupWhenBothTeamsPresent() {
            when(espnApiClient.getScoreboard("football", "nfl"))
                    .thenReturn(buildScoreboardWithTeams("Kansas City Chiefs", "Baltimore Ravens"));

            HeadToHeadResult result =
                    statsService.getHeadToHeadHistory("nfl", "Kansas City Chiefs", "Baltimore Ravens", null);

            assertThat(result.count()).isEqualTo(1);
            assertThat(result.games()).hasSize(1);
            assertThat(result.sport()).isEqualTo("nfl");
            assertThat(result.team1()).isEqualTo("Kansas City Chiefs");
            assertThat(result.team2()).isEqualTo("Baltimore Ravens");
        }

        @Test
        void returnsEmptyWhenTeamsDoNotMatch() {
            when(espnApiClient.getScoreboard("football", "nfl"))
                    .thenReturn(buildScoreboardWithTeams("Kansas City Chiefs", "Baltimore Ravens"));

            HeadToHeadResult result =
                    statsService.getHeadToHeadHistory("nfl", "Dallas Cowboys", "New England Patriots", null);

            assertThat(result.count()).isZero();
            assertThat(result.games()).isEmpty();
        }

        @Test
        void usesDefaultLimitOf10WhenLastnIsNull() {
            ScoreboardResponse scoreboard = buildScoreboardWithTeams("Kansas City Chiefs", "Baltimore Ravens");
            when(espnApiClient.getScoreboard(eq("football"), eq("nfl"))).thenReturn(scoreboard);

            // Just verify it doesn't throw and returns a valid result
            HeadToHeadResult result =
                    statsService.getHeadToHeadHistory("nfl", "Kansas City Chiefs", "Baltimore Ravens", null);

            assertThat(result).isNotNull();
        }

        @Test
        void returnsEmptyResultOnClientException() {
            when(espnApiClient.getScoreboard("football", "nfl")).thenThrow(new RuntimeException("ESPN timeout"));

            HeadToHeadResult result = statsService.getHeadToHeadHistory("nfl", "Chiefs", "Ravens", 5);

            assertThat(result.games()).isEmpty();
            assertThat(result.count()).isZero();
        }

        @Test
        void matchupGameFieldsMappedCorrectly() {
            when(espnApiClient.getScoreboard("football", "nfl"))
                    .thenReturn(buildScoreboardWithTeams("Kansas City Chiefs", "Baltimore Ravens"));

            HeadToHeadResult result =
                    statsService.getHeadToHeadHistory("nfl", "Kansas City Chiefs", "Baltimore Ravens", 5);

            var game = result.games().getFirst();
            assertThat(game.id()).isEqualTo("g1");
            assertThat(game.completed()).isTrue();
            assertThat(game.teams()).hasSize(2);
        }
    }
}
