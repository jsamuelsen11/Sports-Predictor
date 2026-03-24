package com.sportspredictor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.client.espn.EspnApiClient;
import com.sportspredictor.client.espn.ScoreboardResponse;
import com.sportspredictor.service.ScheduleService.GameInfo;
import com.sportspredictor.service.ScheduleService.ScheduleResult;
import com.sportspredictor.service.SportLeagueMapping.LeagueInfo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link ScheduleService}. */
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private EspnApiClient espnApiClient;

    @Mock
    private SportLeagueMapping sportLeagueMapping;

    @InjectMocks
    private ScheduleService scheduleService;

    private static final LeagueInfo NBA_INFO = new LeagueInfo("nba", "basketball", "nba", "basketball_nba", "NBA");

    private static ScoreboardResponse.Event buildEvent(
            String id,
            String name,
            String shortName,
            boolean completed,
            String homeId,
            String homeName,
            String homeAbbr,
            String awayId,
            String awayName,
            String awayAbbr) {
        ScoreboardResponse.Team homeTeam = new ScoreboardResponse.Team(homeId, homeName, homeAbbr);
        ScoreboardResponse.Team awayTeam = new ScoreboardResponse.Team(awayId, awayName, awayAbbr);
        ScoreboardResponse.Competitor homeComp = new ScoreboardResponse.Competitor("c1", homeTeam, "110", false);
        ScoreboardResponse.Competitor awayComp = new ScoreboardResponse.Competitor("c2", awayTeam, "108", true);
        ScoreboardResponse.StatusType statusType = new ScoreboardResponse.StatusType("Final", completed);
        ScoreboardResponse.Status status = new ScoreboardResponse.Status(statusType);
        ScoreboardResponse.Competition comp =
                new ScoreboardResponse.Competition(id, "2026-01-15T21:00:00Z", List.of(homeComp, awayComp), status);
        return new ScoreboardResponse.Event(id, "2026-01-15T21:00:00Z", name, shortName, List.of(comp));
    }

    @BeforeEach
    void setupMapping() {
        when(sportLeagueMapping.resolve("nba")).thenReturn(NBA_INFO);
    }

    @Nested
    class GetGameSchedule {

        @Test
        void withNullDateRangeCallsGetScoreboard() {
            when(espnApiClient.getScoreboard("basketball", "nba")).thenReturn(new ScoreboardResponse(List.of()));

            scheduleService.getGameSchedule("nba", null, null);

            verify(espnApiClient).getScoreboard("basketball", "nba");
        }

        @Test
        void withBlankDateRangeCallsGetScoreboard() {
            when(espnApiClient.getScoreboard("basketball", "nba")).thenReturn(new ScoreboardResponse(List.of()));

            scheduleService.getGameSchedule("nba", "  ", null);

            verify(espnApiClient).getScoreboard("basketball", "nba");
        }

        @Test
        void withDateRangeCallsGetSchedule() {
            when(espnApiClient.getSchedule("basketball", "nba", "20260115"))
                    .thenReturn(new ScoreboardResponse(List.of()));

            scheduleService.getGameSchedule("nba", "20260115", null);

            verify(espnApiClient).getSchedule("basketball", "nba", "20260115");
        }

        @Test
        void returnsAllGamesWhenTeamIsNull() {
            ScoreboardResponse.Event event1 = buildEvent(
                    "g1",
                    "Lakers vs Celtics",
                    "LAL @ BOS",
                    false,
                    "1",
                    "Los Angeles Lakers",
                    "LAL",
                    "2",
                    "Boston Celtics",
                    "BOS");
            ScoreboardResponse.Event event2 = buildEvent(
                    "g2",
                    "Warriors vs Nets",
                    "GSW @ BKN",
                    false,
                    "3",
                    "Golden State Warriors",
                    "GSW",
                    "4",
                    "Brooklyn Nets",
                    "BKN");
            when(espnApiClient.getScoreboard(eq("basketball"), eq("nba")))
                    .thenReturn(new ScoreboardResponse(List.of(event1, event2)));

            ScheduleResult result = scheduleService.getGameSchedule("nba", null, null);

            assertThat(result.games()).hasSize(2);
            assertThat(result.gameCount()).isEqualTo(2);
            assertThat(result.sport()).isEqualTo("nba");
        }

        @Test
        void filtersGamesByTeamDisplayName() {
            ScoreboardResponse.Event lakersGame = buildEvent(
                    "g1",
                    "Lakers vs Celtics",
                    "LAL @ BOS",
                    false,
                    "1",
                    "Los Angeles Lakers",
                    "LAL",
                    "2",
                    "Boston Celtics",
                    "BOS");
            ScoreboardResponse.Event warriorsGame = buildEvent(
                    "g2",
                    "Warriors vs Nets",
                    "GSW @ BKN",
                    false,
                    "3",
                    "Golden State Warriors",
                    "GSW",
                    "4",
                    "Brooklyn Nets",
                    "BKN");
            when(espnApiClient.getScoreboard("basketball", "nba"))
                    .thenReturn(new ScoreboardResponse(List.of(lakersGame, warriorsGame)));

            ScheduleResult result = scheduleService.getGameSchedule("nba", null, "Los Angeles Lakers");

            assertThat(result.games()).hasSize(1);
            assertThat(result.games().getFirst().id()).isEqualTo("g1");
        }

        @Test
        void filtersGamesByTeamAbbreviation() {
            ScoreboardResponse.Event lakersGame = buildEvent(
                    "g1",
                    "Lakers vs Celtics",
                    "LAL @ BOS",
                    false,
                    "1",
                    "Los Angeles Lakers",
                    "LAL",
                    "2",
                    "Boston Celtics",
                    "BOS");
            ScoreboardResponse.Event warriorsGame = buildEvent(
                    "g2",
                    "Warriors vs Nets",
                    "GSW @ BKN",
                    false,
                    "3",
                    "Golden State Warriors",
                    "GSW",
                    "4",
                    "Brooklyn Nets",
                    "BKN");
            when(espnApiClient.getScoreboard("basketball", "nba"))
                    .thenReturn(new ScoreboardResponse(List.of(lakersGame, warriorsGame)));

            ScheduleResult result = scheduleService.getGameSchedule("nba", null, "GSW");

            assertThat(result.games()).hasSize(1);
            assertThat(result.games().getFirst().id()).isEqualTo("g2");
        }

        @Test
        void teamFilterIsCaseInsensitive() {
            ScoreboardResponse.Event lakersGame = buildEvent(
                    "g1",
                    "Lakers vs Celtics",
                    "LAL @ BOS",
                    false,
                    "1",
                    "Los Angeles Lakers",
                    "LAL",
                    "2",
                    "Boston Celtics",
                    "BOS");
            when(espnApiClient.getScoreboard("basketball", "nba"))
                    .thenReturn(new ScoreboardResponse(List.of(lakersGame)));

            ScheduleResult result = scheduleService.getGameSchedule("nba", null, "los angeles lakers");

            assertThat(result.games()).hasSize(1);
        }

        @Test
        void mapsCompletedFlagCorrectly() {
            ScoreboardResponse.Event completedGame = buildEvent(
                    "g1",
                    "Lakers vs Celtics",
                    "LAL @ BOS",
                    true,
                    "1",
                    "Los Angeles Lakers",
                    "LAL",
                    "2",
                    "Boston Celtics",
                    "BOS");
            when(espnApiClient.getScoreboard("basketball", "nba"))
                    .thenReturn(new ScoreboardResponse(List.of(completedGame)));

            ScheduleResult result = scheduleService.getGameSchedule("nba", null, null);

            assertThat(result.games().getFirst().completed()).isTrue();
        }

        @Test
        void returnsEmptyResultOnClientException() {
            when(espnApiClient.getScoreboard("basketball", "nba"))
                    .thenThrow(new RuntimeException("ESPN API unreachable"));

            ScheduleResult result = scheduleService.getGameSchedule("nba", null, null);

            assertThat(result.games()).isEmpty();
            assertThat(result.gameCount()).isZero();
            assertThat(result.sport()).isEqualTo("nba");
        }
    }

    @Nested
    class GetTodaySchedule {

        @Test
        void callsGetGameScheduleWithTodaysDate() {
            when(espnApiClient.getSchedule(eq("basketball"), eq("nba"), org.mockito.ArgumentMatchers.anyString()))
                    .thenReturn(new ScoreboardResponse(List.of()));

            ScheduleResult result = scheduleService.getTodaySchedule("nba");

            assertThat(result.sport()).isEqualTo("nba");
            // Verify getSchedule was called (not getScoreboard), confirming a date was passed
            verify(espnApiClient).getSchedule(eq("basketball"), eq("nba"), org.mockito.ArgumentMatchers.anyString());
        }
    }

    @Nested
    class GetWeekSchedule {

        @Test
        void callsGetGameScheduleWithDateRange() {
            when(espnApiClient.getSchedule(eq("basketball"), eq("nba"), org.mockito.ArgumentMatchers.anyString()))
                    .thenReturn(new ScoreboardResponse(List.of()));

            ScheduleResult result = scheduleService.getWeekSchedule("nba");

            assertThat(result.sport()).isEqualTo("nba");
            verify(espnApiClient).getSchedule(eq("basketball"), eq("nba"), org.mockito.ArgumentMatchers.anyString());
        }

        @Test
        void dateRangeContainsDash() {
            when(espnApiClient.getSchedule(eq("basketball"), eq("nba"), org.mockito.ArgumentMatchers.anyString()))
                    .thenReturn(new ScoreboardResponse(List.of()));

            scheduleService.getWeekSchedule("nba");

            // Capture and verify the date range format includes a dash separator
            org.mockito.ArgumentCaptor<String> captor = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(espnApiClient).getSchedule(eq("basketball"), eq("nba"), captor.capture());
            assertThat(captor.getValue()).contains("-");
        }
    }

    @Nested
    class GameInfoMapping {

        @Test
        void mapsTeamFieldsCorrectly() {
            ScoreboardResponse.Event event = buildEvent(
                    "g1",
                    "Lakers vs Celtics",
                    "LAL @ BOS",
                    false,
                    "101",
                    "Los Angeles Lakers",
                    "LAL",
                    "202",
                    "Boston Celtics",
                    "BOS");
            when(espnApiClient.getScoreboard("basketball", "nba")).thenReturn(new ScoreboardResponse(List.of(event)));

            ScheduleResult result = scheduleService.getGameSchedule("nba", null, null);

            GameInfo game = result.games().getFirst();
            assertThat(game.id()).isEqualTo("g1");
            assertThat(game.name()).isEqualTo("Lakers vs Celtics");
            assertThat(game.shortName()).isEqualTo("LAL @ BOS");
            assertThat(game.teams()).hasSize(2);
            assertThat(game.teams()).extracting("abbreviation").containsExactlyInAnyOrder("LAL", "BOS");
        }
    }
}
