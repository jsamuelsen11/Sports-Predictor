package com.sportspredictor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.client.espn.EspnApiClient;
import com.sportspredictor.client.espn.ScoreboardResponse;
import com.sportspredictor.service.ResultsService.GameResultsResult;
import com.sportspredictor.service.SportLeagueMapping.LeagueInfo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link ResultsService}. */
@ExtendWith(MockitoExtension.class)
class ResultsServiceTest {

    @Mock
    private EspnApiClient espnApiClient;

    @Mock
    private SportLeagueMapping sportLeagueMapping;

    @InjectMocks
    private ResultsService resultsService;

    private static final LeagueInfo NBA_INFO = new LeagueInfo("nba", "basketball", "nba", "basketball_nba", "NBA");

    @BeforeEach
    void setupMapping() {
        when(sportLeagueMapping.resolve("nba")).thenReturn(NBA_INFO);
    }

    // --- Fixture builders ---

    private static ScoreboardResponse.Event buildEvent(String id, String name, boolean completed) {
        ScoreboardResponse.Team homeTeam = new ScoreboardResponse.Team("t1", "Los Angeles Lakers", "LAL");
        ScoreboardResponse.Team awayTeam = new ScoreboardResponse.Team("t2", "Boston Celtics", "BOS");
        ScoreboardResponse.Competitor homeComp = new ScoreboardResponse.Competitor("c1", homeTeam, "110", false);
        ScoreboardResponse.Competitor awayComp = new ScoreboardResponse.Competitor("c2", awayTeam, "108", true);
        ScoreboardResponse.StatusType statusType = new ScoreboardResponse.StatusType("Final", completed);
        ScoreboardResponse.Status status = new ScoreboardResponse.Status(statusType);
        ScoreboardResponse.Competition comp =
                new ScoreboardResponse.Competition(id, "2026-01-15T21:00:00Z", List.of(homeComp, awayComp), status);
        return new ScoreboardResponse.Event(id, "2026-01-15T21:00:00Z", name, "LAL @ BOS", List.of(comp));
    }

    @Nested
    class GetGameResults {

        @Test
        void withNullDateCallsGetScoreboard() {
            when(espnApiClient.getScoreboard("basketball", "nba")).thenReturn(new ScoreboardResponse(List.of()));

            resultsService.getGameResults("nba", null, null);

            verify(espnApiClient).getScoreboard("basketball", "nba");
        }

        @Test
        void withBlankDateCallsGetScoreboard() {
            when(espnApiClient.getScoreboard("basketball", "nba")).thenReturn(new ScoreboardResponse(List.of()));

            resultsService.getGameResults("nba", "  ", null);

            verify(espnApiClient).getScoreboard("basketball", "nba");
        }

        @Test
        void withDateCallsGetSchedule() {
            when(espnApiClient.getSchedule("basketball", "nba", "20260115"))
                    .thenReturn(new ScoreboardResponse(List.of()));

            resultsService.getGameResults("nba", "20260115", null);

            verify(espnApiClient).getSchedule("basketball", "nba", "20260115");
        }

        @Test
        void filtersOutIncompleteGames() {
            ScoreboardResponse.Event completedGame = buildEvent("g1", "Lakers vs Celtics", true);
            ScoreboardResponse.Event inProgressGame = buildEvent("g2", "Warriors vs Nets", false);
            when(espnApiClient.getScoreboard("basketball", "nba"))
                    .thenReturn(new ScoreboardResponse(List.of(completedGame, inProgressGame)));

            GameResultsResult result = resultsService.getGameResults("nba", null, null);

            assertThat(result.results()).hasSize(1);
            assertThat(result.results().getFirst().id()).isEqualTo("g1");
            assertThat(result.gameCount()).isEqualTo(1);
        }

        @Test
        void filtersResultsByEventId() {
            ScoreboardResponse.Event game1 = buildEvent("g1", "Lakers vs Celtics", true);
            ScoreboardResponse.Event game2 = buildEvent("g2", "Warriors vs Nets", true);
            when(espnApiClient.getScoreboard("basketball", "nba"))
                    .thenReturn(new ScoreboardResponse(List.of(game1, game2)));

            GameResultsResult result = resultsService.getGameResults("nba", null, "g2");

            assertThat(result.results()).hasSize(1);
            assertThat(result.results().getFirst().id()).isEqualTo("g2");
        }

        @Test
        void returnsAllCompletedResultsWhenEventIdIsNull() {
            ScoreboardResponse.Event game1 = buildEvent("g1", "Lakers vs Celtics", true);
            ScoreboardResponse.Event game2 = buildEvent("g2", "Warriors vs Nets", true);
            when(espnApiClient.getScoreboard("basketball", "nba"))
                    .thenReturn(new ScoreboardResponse(List.of(game1, game2)));

            GameResultsResult result = resultsService.getGameResults("nba", null, null);

            assertThat(result.results()).hasSize(2);
            assertThat(result.gameCount()).isEqualTo(2);
        }

        @Test
        void returnsEmptyWhenEventIdMatchesNothingCompleted() {
            ScoreboardResponse.Event completedGame = buildEvent("g1", "Lakers vs Celtics", true);
            when(espnApiClient.getScoreboard("basketball", "nba"))
                    .thenReturn(new ScoreboardResponse(List.of(completedGame)));

            GameResultsResult result = resultsService.getGameResults("nba", null, "nonexistent");

            assertThat(result.results()).isEmpty();
            assertThat(result.gameCount()).isZero();
        }

        @Test
        void mapsGameResultFieldsCorrectly() {
            ScoreboardResponse.Event game = buildEvent("g1", "Lakers vs Celtics", true);
            when(espnApiClient.getScoreboard("basketball", "nba")).thenReturn(new ScoreboardResponse(List.of(game)));

            GameResultsResult result = resultsService.getGameResults("nba", null, null);

            var gameResult = result.results().getFirst();
            assertThat(gameResult.id()).isEqualTo("g1");
            assertThat(gameResult.name()).isEqualTo("Lakers vs Celtics");
            assertThat(gameResult.teams()).hasSize(2);

            var homeTeam = gameResult.teams().stream()
                    .filter(t -> t.abbreviation().equals("LAL"))
                    .findFirst()
                    .orElseThrow();
            assertThat(homeTeam.id()).isEqualTo("t1");
            assertThat(homeTeam.displayName()).isEqualTo("Los Angeles Lakers");
            assertThat(homeTeam.score()).isEqualTo("110");
            assertThat(homeTeam.winner()).isFalse();
        }

        @Test
        void setsCorrectSportAndDateOnResult() {
            when(espnApiClient.getSchedule("basketball", "nba", "20260115"))
                    .thenReturn(new ScoreboardResponse(List.of()));

            GameResultsResult result = resultsService.getGameResults("nba", "20260115", null);

            assertThat(result.sport()).isEqualTo("nba");
            assertThat(result.date()).isEqualTo("20260115");
        }

        @Test
        void returnsEmptyResultOnClientException() {
            when(espnApiClient.getScoreboard("basketball", "nba")).thenThrow(new RuntimeException("ESPN unreachable"));

            GameResultsResult result = resultsService.getGameResults("nba", null, null);

            assertThat(result.results()).isEmpty();
            assertThat(result.gameCount()).isZero();
            assertThat(result.sport()).isEqualTo("nba");
        }
    }
}
