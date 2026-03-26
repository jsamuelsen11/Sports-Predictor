package com.sportspredictor.tool;

import com.sportspredictor.service.StatsService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tool for side-by-side statistical comparison between two teams. */
@Service
@RequiredArgsConstructor
public class MatchupTool {

    private final StatsService statsService;

    /** Side-by-side matchup comparison response. */
    public record MatchupResponse(
            String sport,
            TeamProfile team1,
            TeamProfile team2,
            List<StatComparison> comparisons,
            HeadToHeadSummary headToHead,
            String summary) {}

    /** A team's statistical profile. */
    public record TeamProfile(String teamId, List<StatsService.StatSplit> stats) {}

    /** A single stat compared between two teams. */
    public record StatComparison(
            String category,
            String statName,
            double team1Value,
            double team2Value,
            String team1Display,
            String team2Display,
            String advantage) {}

    /** Head-to-head record summary. */
    public record HeadToHeadSummary(int totalGames, int team1Wins, int team2Wins, int draws) {}

    /** Compares two teams side-by-side with detailed statistical breakdowns and head-to-head history. */
    @Tool(
            name = "compare_matchup",
            description = "Compare two teams side-by-side with detailed statistical breakdowns and"
                    + " head-to-head history. Shows offensive, defensive, and overall stats"
                    + " with advantage indicators.")
    public MatchupResponse compareMatchup(
            @ToolParam(description = "Sport key (e.g., nfl, nba, mlb, nhl)") String sport,
            @ToolParam(description = "Team 1 ID") String team1Id,
            @ToolParam(description = "Team 2 ID") String team2Id,
            @ToolParam(
                            description = "Comma-separated stat names to compare (e.g., 'points,rebounds')."
                                    + " Omit for all stats.",
                            required = false)
                    String statsToCompare) {

        StatsService.TeamStatsResult stats1 = statsService.getTeamStats(sport, team1Id, null);
        StatsService.TeamStatsResult stats2 = statsService.getTeamStats(sport, team2Id, null);
        StatsService.HeadToHeadResult h2h = statsService.getHeadToHeadHistory(sport, team1Id, team2Id, null);

        Set<String> filterSet = parseStatsFilter(statsToCompare);
        List<StatComparison> comparisons = buildComparisons(stats1, stats2, filterSet);
        HeadToHeadSummary h2hSummary = buildHeadToHeadSummary(h2h, team1Id);

        long team1Advantages =
                comparisons.stream().filter(c -> "TEAM1".equals(c.advantage())).count();
        long team2Advantages =
                comparisons.stream().filter(c -> "TEAM2".equals(c.advantage())).count();

        String summary = String.format(
                Locale.ROOT,
                "Matchup: %s vs %s (%s). %s leads in %d of %d compared stats."
                        + " H2H: %s %d wins, %s %d wins in %d games.",
                team1Id,
                team2Id,
                sport.toUpperCase(Locale.ROOT),
                team1Advantages >= team2Advantages ? team1Id : team2Id,
                Math.max(team1Advantages, team2Advantages),
                comparisons.size(),
                team1Id,
                h2hSummary.team1Wins(),
                team2Id,
                h2hSummary.team2Wins(),
                h2hSummary.totalGames());

        return new MatchupResponse(
                sport,
                new TeamProfile(team1Id, stats1.splits()),
                new TeamProfile(team2Id, stats2.splits()),
                comparisons,
                h2hSummary,
                summary);
    }

    private static Set<String> parseStatsFilter(String statsToCompare) {
        if (statsToCompare == null || statsToCompare.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(statsToCompare.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private static List<StatComparison> buildComparisons(
            StatsService.TeamStatsResult stats1, StatsService.TeamStatsResult stats2, Set<String> filterSet) {

        List<StatComparison> comparisons = new ArrayList<>();

        for (StatsService.StatSplit split1 : stats1.splits()) {
            StatsService.StatSplit split2 = findMatchingSplit(stats2.splits(), split1.name());
            if (split2 == null) {
                continue;
            }

            for (StatsService.StatCategory cat1 : split1.categories()) {
                StatsService.StatCategory cat2 = findMatchingCategory(split2.categories(), cat1.name());
                if (cat2 == null) {
                    continue;
                }

                for (StatsService.StatEntry entry1 : cat1.stats()) {
                    if (!filterSet.isEmpty()
                            && !filterSet.contains(entry1.name().toLowerCase(Locale.ROOT))) {
                        continue;
                    }
                    StatsService.StatEntry entry2 = findMatchingStat(cat2.stats(), entry1.name());
                    if (entry2 == null) {
                        continue;
                    }

                    String advantage;
                    if (entry1.value() > entry2.value()) {
                        advantage = "TEAM1";
                    } else if (entry2.value() > entry1.value()) {
                        advantage = "TEAM2";
                    } else {
                        advantage = "EVEN";
                    }

                    comparisons.add(new StatComparison(
                            cat1.name(),
                            entry1.name(),
                            entry1.value(),
                            entry2.value(),
                            entry1.displayValue(),
                            entry2.displayValue(),
                            advantage));
                }
            }
        }

        return comparisons;
    }

    private static HeadToHeadSummary buildHeadToHeadSummary(StatsService.HeadToHeadResult h2h, String team1Id) {

        int team1Wins = 0;
        int team2Wins = 0;
        int draws = 0;

        for (StatsService.MatchupGame game : h2h.games()) {
            if (!game.completed()) {
                continue;
            }
            boolean team1Won =
                    game.teams().stream().anyMatch(t -> t.displayName().contains(team1Id) && t.winner());
            boolean team2Won =
                    game.teams().stream().anyMatch(t -> !t.displayName().contains(team1Id) && t.winner());

            if (team1Won) {
                team1Wins++;
            } else if (team2Won) {
                team2Wins++;
            } else {
                draws++;
            }
        }

        return new HeadToHeadSummary(h2h.count(), team1Wins, team2Wins, draws);
    }

    private static StatsService.StatSplit findMatchingSplit(List<StatsService.StatSplit> splits, String name) {
        return splits.stream().filter(s -> s.name().equals(name)).findFirst().orElse(null);
    }

    private static StatsService.StatCategory findMatchingCategory(
            List<StatsService.StatCategory> categories, String name) {
        return categories.stream()
                .filter(c -> c.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    private static StatsService.StatEntry findMatchingStat(List<StatsService.StatEntry> stats, String name) {
        return stats.stream().filter(s -> s.name().equals(name)).findFirst().orElse(null);
    }
}
