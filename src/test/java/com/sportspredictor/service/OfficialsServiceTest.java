package com.sportspredictor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sportspredictor.client.espn.EspnApiClient;
import com.sportspredictor.client.espn.ScoreboardResponse;
import com.sportspredictor.service.OfficialsService.OfficialsResult;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link OfficialsService}. */
@ExtendWith(MockitoExtension.class)
class OfficialsServiceTest {

    @Mock
    private EspnApiClient espnApiClient;

    @Mock
    private SportLeagueMapping sportLeagueMapping;

    @InjectMocks
    private OfficialsService officialsService;

    private static SportLeagueMapping.LeagueInfo nbaInfo() {
        return new SportLeagueMapping.LeagueInfo("nba", "basketball", "nba", "basketball_nba", "NBA");
    }

    private static ScoreboardResponse buildScoreboard(String eventId) {
        ScoreboardResponse.StatusType statusType = new ScoreboardResponse.StatusType("Final", true);
        ScoreboardResponse.Status status = new ScoreboardResponse.Status(statusType);
        ScoreboardResponse.Team team = new ScoreboardResponse.Team("lakers", "Los Angeles Lakers", "LAL");
        ScoreboardResponse.Competitor competitor = new ScoreboardResponse.Competitor("1", team, "110", true);
        ScoreboardResponse.Competition competition =
                new ScoreboardResponse.Competition("comp-1", "2026-03-29", List.of(competitor), status);
        ScoreboardResponse.Event event = new ScoreboardResponse.Event(
                eventId, "2026-03-29", "Lakers vs Celtics", "LAL vs BOS", List.of(competition));
        return new ScoreboardResponse(List.of(event));
    }

    /** Tests for {@link OfficialsService#getOfficials}. */
    @Nested
    class GetOfficials {

        @Test
        void returnsOfficialsResultForKnownEvent() {
            when(sportLeagueMapping.resolve("nba")).thenReturn(nbaInfo());
            when(espnApiClient.getScoreboard(eq("basketball"), eq("nba"))).thenReturn(buildScoreboard("evt-1"));

            OfficialsResult result = officialsService.getOfficials("nba", "evt-1");

            assertThat(result.eventId()).isEqualTo("evt-1");
            assertThat(result.sport()).isEqualTo("nba");
            assertThat(result.summary()).contains("evt-1");
        }

        @Test
        void returnsNotFoundMessageWhenEventNotOnScoreboard() {
            when(sportLeagueMapping.resolve("nba")).thenReturn(nbaInfo());
            when(espnApiClient.getScoreboard(eq("basketball"), eq("nba"))).thenReturn(buildScoreboard("evt-other"));

            OfficialsResult result = officialsService.getOfficials("nba", "evt-missing");

            assertThat(result.officials()).isEmpty();
            assertThat(result.summary()).containsIgnoringCase("not found");
        }

        @Test
        void returnsGracefulResultWhenEspnClientThrows() {
            when(sportLeagueMapping.resolve("nba")).thenReturn(nbaInfo());
            when(espnApiClient.getScoreboard(eq("basketball"), eq("nba")))
                    .thenThrow(new RuntimeException("ESPN timeout"));

            OfficialsResult result = officialsService.getOfficials("nba", "evt-1");

            assertThat(result.officials()).isEmpty();
            assertThat(result.summary()).containsIgnoringCase("failed");
        }

        @Test
        void officialsListIsEmptyForStandardScoreboardEvent() {
            // ESPN scoreboard does not embed official details in standard response
            when(sportLeagueMapping.resolve("nba")).thenReturn(nbaInfo());
            when(espnApiClient.getScoreboard(eq("basketball"), eq("nba"))).thenReturn(buildScoreboard("evt-1"));

            OfficialsResult result = officialsService.getOfficials("nba", "evt-1");

            assertThat(result.officials()).isEmpty();
            assertThat(result.summary()).contains("0 officials");
        }
    }
}
