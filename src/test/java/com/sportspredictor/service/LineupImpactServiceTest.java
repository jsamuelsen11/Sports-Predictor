package com.sportspredictor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sportspredictor.client.espn.EspnApiClient;
import com.sportspredictor.client.espn.RosterResponse;
import com.sportspredictor.service.LineupImpactService.LineupImpactResult;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link LineupImpactService}. */
@ExtendWith(MockitoExtension.class)
class LineupImpactServiceTest {

    @Mock
    private EspnApiClient espnApiClient;

    @Mock
    private SportLeagueMapping sportLeagueMapping;

    @InjectMocks
    private LineupImpactService lineupImpactService;

    private static RosterResponse buildRoster() {
        return new RosterResponse(List.of(
                new RosterResponse.AthleteGroup(
                        "Guards",
                        List.of(
                                new RosterResponse.Athlete("1", "LeBron James", "23", "SF", 39, "250"),
                                new RosterResponse.Athlete("2", "Anthony Davis", "3", "C", 30, "253"),
                                new RosterResponse.Athlete("3", "Austin Reaves", "15", "SG", 26, "197"))),
                new RosterResponse.AthleteGroup(
                        "Forwards",
                        List.of(new RosterResponse.Athlete("4", "D'Angelo Russell", "1", "PG", 28, "193")))));
    }

    private static SportLeagueMapping.LeagueInfo nbaInfo() {
        return new SportLeagueMapping.LeagueInfo("nba", "basketball", "nba", "basketball_nba", "NBA");
    }

    /** Tests for {@link LineupImpactService#getNbaLineupImpact}. */
    @Nested
    class GetNbaLineupImpact {

        @Test
        void returnsPlayersFromRoster() {
            when(sportLeagueMapping.resolve("nba")).thenReturn(nbaInfo());
            when(espnApiClient.getRoster(eq("basketball"), eq("nba"), eq("lakers")))
                    .thenReturn(buildRoster());

            LineupImpactResult result = lineupImpactService.getNbaLineupImpact("lakers");

            assertThat(result.teamId()).isEqualTo("lakers");
            assertThat(result.lineup()).hasSize(4);
            assertThat(result.summary()).contains("lakers");
        }

        @Test
        void offensiveAndDefensiveRatingsAreProportional() {
            when(sportLeagueMapping.resolve("nba")).thenReturn(nbaInfo());
            when(espnApiClient.getRoster(eq("basketball"), eq("nba"), eq("lakers")))
                    .thenReturn(buildRoster());

            LineupImpactResult result = lineupImpactService.getNbaLineupImpact("lakers");

            // offRating = 60% of total, defRating = 40% of total
            assertThat(result.offensiveRating())
                    .isCloseTo(result.projectedImpact() * 0.6, org.assertj.core.data.Offset.offset(0.001));
            assertThat(result.defensiveRating())
                    .isCloseTo(result.projectedImpact() * 0.4, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        void pointGuardHasHigherImpactThanDefaultPosition() {
            when(sportLeagueMapping.resolve("nba")).thenReturn(nbaInfo());
            RosterResponse roster = new RosterResponse(List.of(new RosterResponse.AthleteGroup(
                    "Guards",
                    List.of(
                            new RosterResponse.Athlete("1", "PG Player", "1", "PG", 25, "190"),
                            new RosterResponse.Athlete("2", "Generic Player", "2", "UNKNOWN_POS", 25, "200")))));
            when(espnApiClient.getRoster(eq("basketball"), eq("nba"), eq("team1")))
                    .thenReturn(roster);

            LineupImpactResult result = lineupImpactService.getNbaLineupImpact("team1");

            double pgImpact = result.lineup().stream()
                    .filter(p -> p.playerId().equals("1"))
                    .findFirst()
                    .map(LineupImpactService.LineupPlayer::impactScore)
                    .orElse(0.0);
            double genericImpact = result.lineup().stream()
                    .filter(p -> p.playerId().equals("2"))
                    .findFirst()
                    .map(LineupImpactService.LineupPlayer::impactScore)
                    .orElse(0.0);

            assertThat(pgImpact).isGreaterThan(genericImpact);
        }

        @Test
        void returnsGracefulResultWhenEspnClientThrows() {
            when(sportLeagueMapping.resolve("nba")).thenReturn(nbaInfo());
            when(espnApiClient.getRoster(eq("basketball"), eq("nba"), eq("bad-team")))
                    .thenThrow(new RuntimeException("ESPN API error"));

            LineupImpactResult result = lineupImpactService.getNbaLineupImpact("bad-team");

            assertThat(result.lineup()).isEmpty();
            assertThat(result.projectedImpact()).isEqualTo(0.0);
            assertThat(result.summary()).containsIgnoringCase("failed");
        }

        @Test
        void playersAreSortedByImpactDescending() {
            when(sportLeagueMapping.resolve("nba")).thenReturn(nbaInfo());
            when(espnApiClient.getRoster(eq("basketball"), eq("nba"), eq("lakers")))
                    .thenReturn(buildRoster());

            LineupImpactResult result = lineupImpactService.getNbaLineupImpact("lakers");

            List<Double> scores = result.lineup().stream()
                    .map(LineupImpactService.LineupPlayer::impactScore)
                    .toList();
            for (int i = 0; i < scores.size() - 1; i++) {
                assertThat(scores.get(i)).isGreaterThanOrEqualTo(scores.get(i + 1));
            }
        }
    }
}
