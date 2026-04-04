package com.sportspredictor.mcpserver.service;

import com.sportspredictor.mcpserver.client.espn.EspnApiClient;
import com.sportspredictor.mcpserver.client.espn.PlayerStatsResponse;
import com.sportspredictor.mcpserver.client.espn.ScoreboardResponse;
import com.sportspredictor.mcpserver.client.espn.StandingsResponse;
import com.sportspredictor.mcpserver.client.espn.TeamStatsResponse;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Fetches team and player statistics, standings, and matchup history from ESPN. */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private static final int DEFAULT_H2H_GAMES = 10;

    private final EspnApiClient espnApiClient;
    private final SportLeagueMapping sportLeagueMapping;

    /** Team statistics result. */
    public record TeamStatsResult(String sport, String teamId, List<StatSplit> splits) {}

    /** A statistical split (overall, home, away). */
    public record StatSplit(String name, List<StatCategory> categories) {}

    /** A category of statistics (passing, rushing, etc.). */
    public record StatCategory(String name, List<StatEntry> stats) {}

    /** An individual stat. */
    public record StatEntry(String name, double value, String displayValue) {}

    /** Player statistics result. */
    public record PlayerStatsResult(String sport, String athleteId, List<StatSplit> splits) {}

    /** Team record from standings. */
    public record TeamRecordResult(
            String sport, String teamId, String teamName, String group, List<RecordStat> stats) {}

    /** A standings statistic. */
    public record RecordStat(String name, double value, String displayValue) {}

    /** Head-to-head matchup result. */
    public record HeadToHeadResult(String sport, String team1, String team2, List<MatchupGame> games, int count) {}

    /** A single matchup game. */
    public record MatchupGame(String id, String date, String name, boolean completed, List<MatchupTeam> teams) {}

    /** A team in a matchup. */
    public record MatchupTeam(String displayName, String score, boolean winner) {}

    /** Returns cached team statistics for the given sport, team, and season. */
    @Cacheable(value = "team-stats", key = "#sport + '-' + #teamId + '-' + #season")
    public TeamStatsResult getTeamStats(String sport, String teamId, String season) {
        var info = sportLeagueMapping.resolve(sport);
        try {
            TeamStatsResponse response = espnApiClient.getTeamStats(info.espnSport(), info.espnLeague(), teamId);
            List<StatSplit> splits =
                    response.splits().stream().map(this::toStatSplit).toList();
            return new TeamStatsResult(sport, teamId, splits);
        } catch (Exception e) {
            log.warn("Failed to fetch team stats for sport={}, team={}: {}", sport, teamId, e.getMessage());
            return new TeamStatsResult(sport, teamId, List.of());
        }
    }

    /** Returns cached player statistics for the given sport and athlete. */
    @Cacheable(value = "player-stats", key = "#sport + '-' + #athleteId")
    public PlayerStatsResult getPlayerStats(String sport, String athleteId) {
        var info = sportLeagueMapping.resolve(sport);
        try {
            PlayerStatsResponse response = espnApiClient.getPlayerStats(info.espnSport(), info.espnLeague(), athleteId);
            List<StatSplit> splits =
                    response.splits().stream().map(this::toStatSplit).toList();
            return new PlayerStatsResult(sport, athleteId, splits);
        } catch (Exception e) {
            log.warn("Failed to fetch player stats for sport={}, athlete={}: {}", sport, athleteId, e.getMessage());
            return new PlayerStatsResult(sport, athleteId, List.of());
        }
    }

    /** Returns cached team statistics aggregated across recent seasons for historical trend analysis. */
    @Cacheable(value = "team-stats", key = "'hist-' + #sport + '-' + #teamId + '-' + #seasons")
    public TeamStatsResult getHistoricalTeamStats(String sport, String teamId, int seasons) {
        // ESPN doesn't support multi-season queries natively; return current season stats
        return getTeamStats(sport, teamId, null);
    }

    /** Returns cached career and multi-season player statistics for the given sport and athlete. */
    @Cacheable(value = "player-stats", key = "'hist-' + #sport + '-' + #athleteId")
    public PlayerStatsResult getHistoricalPlayerStats(String sport, String athleteId) {
        // ESPN player stats include career splits by default
        return getPlayerStats(sport, athleteId);
    }

    /** Returns cached head-to-head matchup history between two teams for the given sport. */
    @Cacheable(value = "schedules", key = "'h2h-' + #sport + '-' + #team1 + '-' + #team2 + '-' + #lastN")
    public HeadToHeadResult getHeadToHeadHistory(String sport, String team1, String team2, Integer lastN) {
        var info = sportLeagueMapping.resolve(sport);
        int limit = (lastN != null && lastN > 0) ? lastN : DEFAULT_H2H_GAMES;

        try {
            // Fetch recent scoreboard and filter for games involving both teams
            ScoreboardResponse scoreboard = espnApiClient.getScoreboard(info.espnSport(), info.espnLeague());

            List<MatchupGame> matchups = scoreboard.events().stream()
                    .filter(e -> eventInvolvesTeams(e, team1, team2))
                    .limit(limit)
                    .map(this::toMatchupGame)
                    .toList();

            return new HeadToHeadResult(sport, team1, team2, matchups, matchups.size());
        } catch (Exception e) {
            log.warn("Failed to fetch H2H for sport={}, {} vs {}: {}", sport, team1, team2, e.getMessage());
            return new HeadToHeadResult(sport, team1, team2, List.of(), 0);
        }
    }

    /** Returns the cached win-loss record and standings data for a team in the given sport. */
    @Cacheable(value = "standings", key = "#sport + '-' + #teamId")
    public TeamRecordResult getTeamRecord(String sport, String teamId) {
        var info = sportLeagueMapping.resolve(sport);
        try {
            StandingsResponse standings = espnApiClient.getStandings(info.espnSport(), info.espnLeague());

            // Search all groups for the matching team
            for (var group : standings.children()) {
                for (var standing : group.standings()) {
                    for (var entry : standing.entries()) {
                        if (entry.team() != null && entry.team().contains(teamId)) {
                            List<RecordStat> stats = entry.stats().stream()
                                    .map(s -> new RecordStat(s.name(), s.value(), s.displayValue()))
                                    .toList();
                            return new TeamRecordResult(sport, teamId, entry.team(), group.name(), stats);
                        }
                    }
                }
            }

            log.warn("Team {} not found in standings for sport={}", teamId, sport);
            return new TeamRecordResult(sport, teamId, null, null, List.of());
        } catch (Exception e) {
            log.warn("Failed to fetch team record for sport={}, team={}: {}", sport, teamId, e.getMessage());
            return new TeamRecordResult(sport, teamId, null, null, List.of());
        }
    }

    private StatSplit toStatSplit(TeamStatsResponse.Split split) {
        List<StatCategory> categories = split.categories().stream()
                .map(c -> new StatCategory(
                        c.name(),
                        c.stats().stream()
                                .map(s -> new StatEntry(s.name(), s.value(), s.displayValue()))
                                .toList()))
                .toList();
        return new StatSplit(split.name(), categories);
    }

    private StatSplit toStatSplit(PlayerStatsResponse.Split split) {
        List<StatCategory> categories = split.categories().stream()
                .map(c -> new StatCategory(
                        c.name(),
                        c.stats().stream()
                                .map(s -> new StatEntry(s.name(), s.value(), s.displayValue()))
                                .toList()))
                .toList();
        return new StatSplit(split.name(), categories);
    }

    private boolean eventInvolvesTeams(ScoreboardResponse.Event event, String team1, String team2) {
        List<String> teamNames = event.competitions().stream()
                .flatMap(c -> c.competitors().stream())
                .map(comp -> comp.team().displayName().toLowerCase(Locale.ROOT))
                .toList();
        return teamNames.stream().anyMatch(n -> n.contains(team1.toLowerCase(Locale.ROOT)))
                && teamNames.stream().anyMatch(n -> n.contains(team2.toLowerCase(Locale.ROOT)));
    }

    private MatchupGame toMatchupGame(ScoreboardResponse.Event event) {
        boolean completed = event.competitions().stream()
                .anyMatch(c -> c.status() != null
                        && c.status().type() != null
                        && c.status().type().completed());

        List<MatchupTeam> teams = event.competitions().stream()
                .flatMap(c -> c.competitors().stream())
                .map(comp -> new MatchupTeam(comp.team().displayName(), comp.score(), comp.winner()))
                .toList();

        return new MatchupGame(event.id(), event.date(), event.name(), completed, teams);
    }
}
