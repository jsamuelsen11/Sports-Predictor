package com.sportspredictor.tool.ingestion;

import com.sportspredictor.service.StatsService;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tools for team stats, player stats, standings, and matchup history. */
@Service
@RequiredArgsConstructor
public class StatsIngestionTool {

    private static final int DEFAULT_SEASONS = 3;
    private static final int DEFAULT_H2H_GAMES = 10;

    private final StatsService statsService;

    /** Response for team stats queries. */
    public record TeamStatsResponse(String sport, String teamId, List<StatsService.StatSplit> splits, String summary) {}

    /** Response for player stats queries. */
    public record PlayerStatsResponse(
            String sport, String athleteId, List<StatsService.StatSplit> splits, String summary) {}

    /** Response for head-to-head queries. */
    public record HeadToHeadResponse(
            String sport,
            String team1,
            String team2,
            List<StatsService.MatchupGame> games,
            int count,
            String summary) {}

    /** Response for team record queries. */
    public record TeamRecordResponse(
            String sport,
            String teamId,
            String teamName,
            String group,
            List<StatsService.RecordStat> stats,
            String summary) {}

    /** Fetches team statistics for a sport including offensive, defensive, and split metrics. */
    @Tool(
            name = "get_team_stats",
            description = "Get team statistics for a sport including offensive and defensive metrics,"
                    + " with splits for overall, home, and away performance.")
    public TeamStatsResponse getTeamStats(
            @ToolParam(description = "Sport key (e.g., nfl, nba, mlb, nhl, epl)") String sport,
            @ToolParam(description = "ESPN team ID") String teamId,
            @ToolParam(description = "Season year (e.g., 2026)", required = false) String season) {
        var result = statsService.getTeamStats(sport, teamId, season);

        long statCount = result.splits().stream()
                .mapToLong(s ->
                        s.categories().stream().mapToLong(c -> c.stats().size()).sum())
                .sum();

        String summary;
        if (result.splits().isEmpty()) {
            summary = String.format(Locale.ROOT, "No stats found for team %s in %s.", teamId, sport);
        } else {
            summary = String.format(
                    Locale.ROOT,
                    "Team %s %s stats: %d splits with %d total statistics.",
                    teamId,
                    sport.toUpperCase(Locale.ROOT),
                    result.splits().size(),
                    statCount);
        }

        return new TeamStatsResponse(result.sport(), result.teamId(), result.splits(), summary);
    }

    /** Fetches individual player statistics including season averages and career numbers. */
    @Tool(
            name = "get_player_stats",
            description = "Get individual player statistics including season averages and career numbers.")
    public PlayerStatsResponse getPlayerStats(
            @ToolParam(description = "Sport key (e.g., nfl, nba, mlb, nhl, epl)") String sport,
            @ToolParam(description = "ESPN athlete ID") String athleteId) {
        var result = statsService.getPlayerStats(sport, athleteId);

        String summary;
        if (result.splits().isEmpty()) {
            summary = String.format(Locale.ROOT, "No stats found for athlete %s in %s.", athleteId, sport);
        } else {
            summary = String.format(
                    Locale.ROOT,
                    "Athlete %s %s stats: %d splits available.",
                    athleteId,
                    sport.toUpperCase(Locale.ROOT),
                    result.splits().size());
        }

        return new PlayerStatsResponse(result.sport(), result.athleteId(), result.splits(), summary);
    }

    /** Fetches team statistics across multiple recent seasons for trend analysis. */
    @Tool(
            name = "get_historical_team_stats",
            description = "Get team statistics across multiple recent seasons for trend analysis.")
    public TeamStatsResponse getHistoricalTeamStats(
            @ToolParam(description = "Sport key (e.g., nfl, nba)") String sport,
            @ToolParam(description = "ESPN team ID") String teamId,
            @ToolParam(description = "Number of seasons to retrieve (default: 3)", required = false)
                    Integer numSeasons) {
        int seasons = (numSeasons != null && numSeasons > 0) ? numSeasons : DEFAULT_SEASONS;
        var result = statsService.getHistoricalTeamStats(sport, teamId, seasons);

        String summary;
        if (result.splits().isEmpty()) {
            summary = String.format(Locale.ROOT, "No historical stats found for team %s in %s.", teamId, sport);
        } else {
            summary = String.format(
                    Locale.ROOT,
                    "Historical stats for team %s in %s (requested %d seasons).",
                    teamId,
                    sport.toUpperCase(Locale.ROOT),
                    seasons);
        }

        return new TeamStatsResponse(result.sport(), result.teamId(), result.splits(), summary);
    }

    /** Fetches career and multi-season player statistics for historical analysis. */
    @Tool(
            name = "get_historical_player_stats",
            description = "Get career and multi-season player statistics for historical analysis.")
    public PlayerStatsResponse getHistoricalPlayerStats(
            @ToolParam(description = "Sport key (e.g., nfl, nba)") String sport,
            @ToolParam(description = "ESPN athlete ID") String athleteId) {
        var result = statsService.getHistoricalPlayerStats(sport, athleteId);

        String summary;
        if (result.splits().isEmpty()) {
            summary = String.format(Locale.ROOT, "No historical stats found for athlete %s in %s.", athleteId, sport);
        } else {
            summary = String.format(
                    Locale.ROOT,
                    "Historical stats for athlete %s in %s: %d splits (includes career data).",
                    athleteId,
                    sport.toUpperCase(Locale.ROOT),
                    result.splits().size());
        }

        return new PlayerStatsResponse(result.sport(), result.athleteId(), result.splits(), summary);
    }

    /** Fetches recent head-to-head matchup history between two teams. */
    @Tool(
            name = "get_head_to_head_history",
            description = "Get recent matchup history between two teams showing results of their last meetings.")
    public HeadToHeadResponse getHeadToHeadHistory(
            @ToolParam(description = "Sport key (e.g., nfl, nba)") String sport,
            @ToolParam(description = "First team name") String team1,
            @ToolParam(description = "Second team name") String team2,
            @ToolParam(description = "Number of recent matchups (default: 10)", required = false) Integer lastN) {
        int limit = (lastN != null && lastN > 0) ? lastN : DEFAULT_H2H_GAMES;
        var result = statsService.getHeadToHeadHistory(sport, team1, team2, limit);

        String summary;
        if (result.count() == 0) {
            summary = String.format(Locale.ROOT, "No matchup history found for %s vs %s in %s.", team1, team2, sport);
        } else {
            summary = String.format(
                    Locale.ROOT,
                    "Found %d recent %s matchups between %s and %s.",
                    result.count(),
                    sport.toUpperCase(Locale.ROOT),
                    team1,
                    team2);
        }

        return new HeadToHeadResponse(
                result.sport(), result.team1(), result.team2(), result.games(), result.count(), summary);
    }

    /** Looks up a team's win-loss record from current standings with conference and division grouping. */
    @Tool(
            name = "lookup_team_record",
            description = "Look up a team's win-loss record from current standings with conference/division grouping.")
    public TeamRecordResponse lookupTeamRecord(
            @ToolParam(description = "Sport key (e.g., nfl, nba)") String sport,
            @ToolParam(description = "ESPN team ID") String teamId) {
        var result = statsService.getTeamRecord(sport, teamId);

        String summary;
        if (result.stats().isEmpty()) {
            summary = String.format(Locale.ROOT, "No record found for team %s in %s.", teamId, sport);
        } else {
            summary = String.format(
                    Locale.ROOT,
                    "Team %s in %s (%s): %d record statistics available.",
                    result.teamName() != null ? result.teamName() : teamId,
                    sport.toUpperCase(Locale.ROOT),
                    result.group() != null ? result.group() : "unknown group",
                    result.stats().size());
        }

        return new TeamRecordResponse(
                result.sport(), result.teamId(), result.teamName(), result.group(), result.stats(), summary);
    }
}
