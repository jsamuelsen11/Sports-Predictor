package com.sportspredictor.mcpserver.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Analyzes performance trends for teams over configurable time windows. */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrendService {

    private static final int LAST_5 = 5;
    private static final int LAST_10 = 10;
    private static final int LAST_20 = 20;
    private static final int SEASON_SIZE = 999;
    private static final String DEFAULT_WINDOW = "last_10";
    private static final double TREND_THRESHOLD = 0.02;
    private static final double EVEN_BASELINE = 0.50;
    private static final double PERCENT_MULTIPLIER = 100.0;

    private final StatsService statsService;
    private final ResultsService resultsService;

    /** Trend analysis result for a team over a window. */
    public record TrendResult(
            String sport,
            String teamId,
            String window,
            List<TrendMetric> metrics,
            RecordTrend record,
            String summary) {}

    /** A single metric with trend direction. */
    public record TrendMetric(
            String name, String category, double currentValue, String displayValue, String direction) {}

    /** Win-loss record trend. */
    public record RecordTrend(int wins, int losses, double winRate, String trend) {}

    /** Analyzes performance trends for a team over a specified window. */
    @Cacheable(value = "team-stats", key = "'trends-' + #sport + '-' + #teamId + '-' + #window")
    public TrendResult analyzeTrends(String sport, String teamId, String window, List<String> metrics) {
        String resolvedWindow = (window != null && !window.isBlank()) ? window : DEFAULT_WINDOW;

        try {
            StatsService.TeamStatsResult stats = statsService.getTeamStats(sport, teamId, null);
            ResultsService.GameResultsResult results = resultsService.getGameResults(sport, null, null);

            List<TrendMetric> trendMetrics = buildTrendMetrics(stats, metrics);
            RecordTrend recordTrend = buildRecordTrend(results, teamId, parseWindowSize(resolvedWindow));

            String summary = String.format(
                    Locale.ROOT,
                    "Trends for team %s (%s) over %s: %d metrics analyzed. Record: %d-%d (%.1f%% win rate, %s).",
                    teamId,
                    sport.toUpperCase(Locale.ROOT),
                    resolvedWindow,
                    trendMetrics.size(),
                    recordTrend.wins(),
                    recordTrend.losses(),
                    recordTrend.winRate() * PERCENT_MULTIPLIER,
                    recordTrend.trend());

            return new TrendResult(sport, teamId, resolvedWindow, trendMetrics, recordTrend, summary);
        } catch (Exception e) {
            log.warn("Failed to analyze trends for sport={}, team={}: {}", sport, teamId, e.getMessage());
            RecordTrend emptyRecord = new RecordTrend(0, 0, 0.0, "FLAT");
            return new TrendResult(sport, teamId, resolvedWindow, List.of(), emptyRecord, "No trend data available.");
        }
    }

    private List<TrendMetric> buildTrendMetrics(StatsService.TeamStatsResult stats, List<String> metricFilter) {
        List<TrendMetric> trendMetrics = new ArrayList<>();

        for (StatsService.StatSplit split : stats.splits()) {
            for (StatsService.StatCategory category : split.categories()) {
                for (StatsService.StatEntry entry : category.stats()) {
                    if (metricFilter != null
                            && !metricFilter.isEmpty()
                            && metricFilter.stream().noneMatch(m -> entry.name()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(m.toLowerCase(Locale.ROOT)))) {
                        continue;
                    }

                    String direction = determineTrend(entry.value(), 0.0);
                    trendMetrics.add(new TrendMetric(
                            entry.name(), category.name(), entry.value(), entry.displayValue(), direction));
                }
            }
        }

        return trendMetrics;
    }

    private RecordTrend buildRecordTrend(ResultsService.GameResultsResult results, String teamId, int windowSize) {
        List<ResultsService.GameResult> teamGames = filterResultsForTeam(results.results(), teamId);

        int limit = Math.min(windowSize, teamGames.size());
        List<ResultsService.GameResult> recentGames = teamGames.subList(0, limit);

        int wins = 0;
        int losses = 0;

        for (ResultsService.GameResult game : recentGames) {
            boolean teamWon = game.teams().stream()
                    .anyMatch(t -> (t.id().equals(teamId)
                                    || t.displayName()
                                            .toLowerCase(Locale.ROOT)
                                            .contains(teamId.toLowerCase(Locale.ROOT))
                                    || t.abbreviation().equalsIgnoreCase(teamId))
                            && t.winner());
            if (teamWon) {
                wins++;
            } else {
                losses++;
            }
        }

        double winRate = (wins + losses) > 0 ? (double) wins / (wins + losses) : 0.0;
        String trend = determineTrend(winRate, EVEN_BASELINE);

        return new RecordTrend(wins, losses, winRate, trend);
    }

    private static List<ResultsService.GameResult> filterResultsForTeam(
            List<ResultsService.GameResult> results, String teamId) {
        return results.stream()
                .filter(r -> r.teams().stream()
                        .anyMatch(t -> t.id().equals(teamId)
                                || t.displayName().toLowerCase(Locale.ROOT).contains(teamId.toLowerCase(Locale.ROOT))
                                || t.abbreviation().equalsIgnoreCase(teamId)))
                .toList();
    }

    static int parseWindowSize(String window) {
        if (window == null) {
            return LAST_10;
        }
        return switch (window.toLowerCase(Locale.ROOT)) {
            case "last_5" -> LAST_5;
            case "last_10" -> LAST_10;
            case "last_20" -> LAST_20;
            case "season" -> SEASON_SIZE;
            default -> LAST_10;
        };
    }

    private static String determineTrend(double value, double baseline) {
        if (value > baseline + TREND_THRESHOLD) {
            return "UP";
        } else if (value < baseline - TREND_THRESHOLD) {
            return "DOWN";
        } else {
            return "FLAT";
        }
    }
}
