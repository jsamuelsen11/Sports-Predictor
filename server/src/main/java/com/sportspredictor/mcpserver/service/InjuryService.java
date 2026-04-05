package com.sportspredictor.mcpserver.service;

import com.sportspredictor.mcpserver.client.espn.EspnApiClient;
import com.sportspredictor.mcpserver.client.espn.InjuryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Fetches injury reports from ESPN with caching and team filtering. */
@Service
@RequiredArgsConstructor
@Slf4j
public class InjuryService {

    private final EspnApiClient espnApiClient;
    private final SportLeagueMapping sportLeagueMapping;

    /** Injury report result. */
    public record InjuryReportResult(String sport, String team, List<TeamInjuryInfo> teamInjuries, int totalInjuries) {}

    /** Injuries for a single team. */
    public record TeamInjuryInfo(String teamId, String teamName, String abbreviation, List<InjuryInfo> injuries) {}

    /** An individual injury entry. */
    public record InjuryInfo(
            String athleteId, String athleteName, String position, String status, String description) {}

    /** Returns a cached injury report for the given sport with optional team filtering. */
    @Cacheable(value = "team-stats", key = "'injuries-' + #sport + '-' + #team")
    public InjuryReportResult getInjuryReport(String sport, String team) {
        var info = sportLeagueMapping.resolve(sport);

        try {
            InjuryResponse response = espnApiClient.getInjuries(info.espnSport(), info.espnLeague());

            List<TeamInjuryInfo> teamInjuries = response.injuries().stream()
                    .filter(ti -> team == null
                            || team.isBlank()
                            || ti.team().displayName().equalsIgnoreCase(team)
                            || ti.team().abbreviation().equalsIgnoreCase(team))
                    .map(this::toTeamInjuryInfo)
                    .toList();

            int totalInjuries =
                    teamInjuries.stream().mapToInt(ti -> ti.injuries().size()).sum();

            return new InjuryReportResult(sport, team, teamInjuries, totalInjuries);
        } catch (Exception e) {
            log.warn("Failed to fetch injury report for sport={}: {}", sport, e.getMessage());
            return new InjuryReportResult(sport, team, List.of(), 0);
        }
    }

    private TeamInjuryInfo toTeamInjuryInfo(InjuryResponse.TeamInjuries ti) {
        List<InjuryInfo> injuries = ti.injuries().stream()
                .map(i -> new InjuryInfo(
                        i.athlete().id(),
                        i.athlete().displayName(),
                        i.athlete().position(),
                        i.status(),
                        i.description()))
                .toList();
        return new TeamInjuryInfo(
                ti.team().id(), ti.team().displayName(), ti.team().abbreviation(), injuries);
    }
}
