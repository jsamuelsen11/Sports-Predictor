package com.sportspredictor.tool;

import com.sportspredictor.service.TrendService;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tool for analyzing performance trends over a configurable time window. */
@Service
@RequiredArgsConstructor
public class TrendTool {

    private final TrendService trendService;

    /** Trend analysis response wrapping the service result. */
    public record TrendResponse(
            String sport,
            String teamId,
            String window,
            List<TrendService.TrendMetric> metrics,
            TrendService.RecordTrend record,
            String summary) {}

    /** Analyzes performance trends for a team over a specified window. */
    @Tool(
            name = "analyze_trends",
            description = "Analyze performance trends for a team over a specified window. Shows statistical"
                    + " trends with UP/DOWN/FLAT indicators, recent win-loss record, and"
                    + " momentum analysis.")
    public TrendResponse analyzeTrends(
            @ToolParam(description = "Sport key (e.g., nfl, nba, mlb, nhl)") String sport,
            @ToolParam(description = "Team ID") String teamId,
            @ToolParam(description = "Analysis window: last_5, last_10, last_20, or season", required = false)
                    String window,
            @ToolParam(
                            description = "Comma-separated metric names to include (e.g., 'points,rebounds')."
                                    + " Omit for all.",
                            required = false)
                    String metrics) {

        List<String> metricList = parseMetrics(metrics);
        TrendService.TrendResult result = trendService.analyzeTrends(sport, teamId, window, metricList);

        return new TrendResponse(
                result.sport(), result.teamId(), result.window(), result.metrics(), result.record(), result.summary());
    }

    private static List<String> parseMetrics(String metrics) {
        if (metrics == null || metrics.isBlank()) {
            return null;
        }
        return Arrays.stream(metrics.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
