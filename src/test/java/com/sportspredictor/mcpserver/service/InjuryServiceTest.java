package com.sportspredictor.mcpserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.mcpserver.client.espn.EspnApiClient;
import com.sportspredictor.mcpserver.client.espn.InjuryResponse;
import com.sportspredictor.mcpserver.service.InjuryService.InjuryReportResult;
import com.sportspredictor.mcpserver.service.SportLeagueMapping.LeagueInfo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link InjuryService}. */
@ExtendWith(MockitoExtension.class)
class InjuryServiceTest {

    @Mock
    private EspnApiClient espnApiClient;

    @Mock
    private SportLeagueMapping sportLeagueMapping;

    @InjectMocks
    private InjuryService injuryService;

    private static final LeagueInfo NFL_INFO = new LeagueInfo("nfl", "football", "nfl", "americanfootball_nfl", "NFL");

    @BeforeEach
    void setupMapping() {
        when(sportLeagueMapping.resolve("nfl")).thenReturn(NFL_INFO);
    }

    // --- Fixture builders ---

    private static InjuryResponse.TeamInjuries buildTeamInjuries(
            String teamId, String teamName, String teamAbbr, int injuryCount) {
        InjuryResponse.Team team = new InjuryResponse.Team(teamId, teamName, teamAbbr);
        List<InjuryResponse.Injury> injuries = java.util.stream.IntStream.range(0, injuryCount)
                .mapToObj(i -> {
                    InjuryResponse.Athlete athlete = new InjuryResponse.Athlete("a" + i, "Player " + i, "WR");
                    return new InjuryResponse.Injury(athlete, "Out", "2026-01-10", "Knee injury");
                })
                .toList();
        return new InjuryResponse.TeamInjuries(team, injuries);
    }

    @Nested
    class GetInjuryReport {

        @Test
        void delegatesToClientWithCorrectEspnIds() {
            when(espnApiClient.getInjuries("football", "nfl")).thenReturn(new InjuryResponse(List.of()));

            injuryService.getInjuryReport("nfl", null);

            verify(espnApiClient).getInjuries("football", "nfl");
        }

        @Test
        void returnsAllTeamsWhenTeamFilterIsNull() {
            InjuryResponse.TeamInjuries chiefs = buildTeamInjuries("1", "Kansas City Chiefs", "KC", 2);
            InjuryResponse.TeamInjuries eagles = buildTeamInjuries("2", "Philadelphia Eagles", "PHI", 3);
            when(espnApiClient.getInjuries("football", "nfl")).thenReturn(new InjuryResponse(List.of(chiefs, eagles)));

            InjuryReportResult result = injuryService.getInjuryReport("nfl", null);

            assertThat(result.teamInjuries()).hasSize(2);
            assertThat(result.totalInjuries()).isEqualTo(5);
            assertThat(result.sport()).isEqualTo("nfl");
            assertThat(result.team()).isNull();
        }

        @Test
        void returnsAllTeamsWhenTeamFilterIsBlank() {
            InjuryResponse.TeamInjuries chiefs = buildTeamInjuries("1", "Kansas City Chiefs", "KC", 2);
            when(espnApiClient.getInjuries("football", "nfl")).thenReturn(new InjuryResponse(List.of(chiefs)));

            InjuryReportResult result = injuryService.getInjuryReport("nfl", "  ");

            assertThat(result.teamInjuries()).hasSize(1);
        }

        @Test
        void filtersByTeamDisplayName() {
            InjuryResponse.TeamInjuries chiefs = buildTeamInjuries("1", "Kansas City Chiefs", "KC", 2);
            InjuryResponse.TeamInjuries eagles = buildTeamInjuries("2", "Philadelphia Eagles", "PHI", 3);
            when(espnApiClient.getInjuries("football", "nfl")).thenReturn(new InjuryResponse(List.of(chiefs, eagles)));

            InjuryReportResult result = injuryService.getInjuryReport("nfl", "Kansas City Chiefs");

            assertThat(result.teamInjuries()).hasSize(1);
            assertThat(result.teamInjuries().getFirst().teamName()).isEqualTo("Kansas City Chiefs");
            assertThat(result.totalInjuries()).isEqualTo(2);
        }

        @Test
        void filtersByTeamAbbreviation() {
            InjuryResponse.TeamInjuries chiefs = buildTeamInjuries("1", "Kansas City Chiefs", "KC", 2);
            InjuryResponse.TeamInjuries eagles = buildTeamInjuries("2", "Philadelphia Eagles", "PHI", 3);
            when(espnApiClient.getInjuries("football", "nfl")).thenReturn(new InjuryResponse(List.of(chiefs, eagles)));

            InjuryReportResult result = injuryService.getInjuryReport("nfl", "PHI");

            assertThat(result.teamInjuries()).hasSize(1);
            assertThat(result.teamInjuries().getFirst().abbreviation()).isEqualTo("PHI");
            assertThat(result.totalInjuries()).isEqualTo(3);
        }

        @Test
        void teamFilterIsCaseInsensitive() {
            InjuryResponse.TeamInjuries chiefs = buildTeamInjuries("1", "Kansas City Chiefs", "KC", 1);
            when(espnApiClient.getInjuries("football", "nfl")).thenReturn(new InjuryResponse(List.of(chiefs)));

            InjuryReportResult result = injuryService.getInjuryReport("nfl", "kansas city chiefs");

            assertThat(result.teamInjuries()).hasSize(1);
        }

        @Test
        void mapsInjuryFieldsCorrectly() {
            InjuryResponse.TeamInjuries chiefs = buildTeamInjuries("1", "Kansas City Chiefs", "KC", 1);
            when(espnApiClient.getInjuries("football", "nfl")).thenReturn(new InjuryResponse(List.of(chiefs)));

            InjuryReportResult result = injuryService.getInjuryReport("nfl", null);

            var teamInfo = result.teamInjuries().getFirst();
            assertThat(teamInfo.teamId()).isEqualTo("1");
            assertThat(teamInfo.teamName()).isEqualTo("Kansas City Chiefs");
            assertThat(teamInfo.abbreviation()).isEqualTo("KC");
            assertThat(teamInfo.injuries()).hasSize(1);

            var injury = teamInfo.injuries().getFirst();
            assertThat(injury.athleteId()).isEqualTo("a0");
            assertThat(injury.athleteName()).isEqualTo("Player 0");
            assertThat(injury.position()).isEqualTo("WR");
            assertThat(injury.status()).isEqualTo("Out");
            assertThat(injury.description()).isEqualTo("Knee injury");
        }

        @Test
        void returnsEmptyResultOnClientException() {
            when(espnApiClient.getInjuries("football", "nfl")).thenThrow(new RuntimeException("ESPN unavailable"));

            InjuryReportResult result = injuryService.getInjuryReport("nfl", null);

            assertThat(result.teamInjuries()).isEmpty();
            assertThat(result.totalInjuries()).isZero();
            assertThat(result.sport()).isEqualTo("nfl");
        }

        @Test
        void returnsEmptyResultWhenNoTeamMatchesFilter() {
            InjuryResponse.TeamInjuries chiefs = buildTeamInjuries("1", "Kansas City Chiefs", "KC", 2);
            when(espnApiClient.getInjuries("football", "nfl")).thenReturn(new InjuryResponse(List.of(chiefs)));

            InjuryReportResult result = injuryService.getInjuryReport("nfl", "Dallas Cowboys");

            assertThat(result.teamInjuries()).isEmpty();
            assertThat(result.totalInjuries()).isZero();
        }
    }
}
