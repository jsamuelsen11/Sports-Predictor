package com.sportspredictor.mcpserver.tool;

import com.sportspredictor.mcpserver.service.PerformanceAnalyticsService;
import com.sportspredictor.mcpserver.service.PerformanceAnalyticsService.ExportResult;
import com.sportspredictor.mcpserver.service.PerformanceAnalyticsService.ParlayResult;
import com.sportspredictor.mcpserver.service.PerformanceAnalyticsService.ProfitGraphResult;
import com.sportspredictor.mcpserver.service.PerformanceAnalyticsService.RoiResult;
import com.sportspredictor.mcpserver.service.PerformanceAnalyticsService.SeasonSummaryResult;
import com.sportspredictor.mcpserver.service.PerformanceAnalyticsService.StreakResult;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tools for performance analytics. */
@Service
@RequiredArgsConstructor
public class PerformanceAnalyticsTool {

    private final PerformanceAnalyticsService analyticsService;

    /** Response records. */
    public record StreakResponse(
            int currentStreak, String currentType, int longestWin, int longestLoss, String summary) {}

    /** ROI breakdown response. */
    public record RoiResponse(
            Map<String, Double> roiBySport, Map<String, Double> roiByBetType, double overallRoi, String summary) {}

    /** Parlay performance response. */
    public record ParlayResponse(
            int totalParlays, double hitRate, double avgPayout, double bestPayout, String summary) {}

    /** Profit graph response. */
    public record ProfitGraphResponse(int dataPointCount, String granularity, String summary) {}

    /** Season summary response. */
    public record SeasonResponse(
            int totalBets, int wins, int losses, int pushes, double netProfit, double roi, String summary) {}

    /** Export response. */
    public record ExportResponse(String data, String format, int count, String summary) {}

    /** Gets streak history. */
    @Tool(name = "get_streak_history", description = "Win/loss streak analysis over time")
    public StreakResponse getStreakHistory(@ToolParam(description = "Bankroll ID") String bankrollId) {
        StreakResult r = analyticsService.getStreakHistory(bankrollId);
        return new StreakResponse(
                r.currentStreak(), r.currentType(), r.longestWinStreak(), r.longestLossStreak(), r.summary());
    }

    /** Gets ROI by category. */
    @Tool(name = "get_roi_by_category", description = "ROI breakdown by sport, bet type, and overall")
    public RoiResponse getRoiByCategory(@ToolParam(description = "Bankroll ID") String bankrollId) {
        RoiResult r = analyticsService.getRoiByCategory(bankrollId);
        return new RoiResponse(r.roiBySport(), r.roiByBetType(), r.overallRoi(), r.summary());
    }

    /** Gets parlay performance stats. */
    @Tool(name = "get_parlay_performance", description = "Parlay-specific analytics: hit rate, payouts")
    public ParlayResponse getParlayPerformance(@ToolParam(description = "Bankroll ID") String bankrollId) {
        ParlayResult r = analyticsService.getParlayPerformance(bankrollId);
        return new ParlayResponse(r.totalParlays(), r.hitRate(), r.avgPayout(), r.bestPayout(), r.summary());
    }

    /** Gets profit graph data points. */
    @Tool(name = "get_profit_graph_data", description = "Time-series bankroll balance data for charting")
    public ProfitGraphResponse getProfitGraphData(
            @ToolParam(description = "Bankroll ID") String bankrollId,
            @ToolParam(description = "Granularity: daily, weekly, monthly") String granularity) {
        ProfitGraphResult r = analyticsService.getProfitGraphData(bankrollId, granularity);
        return new ProfitGraphResponse(r.dataPoints().size(), r.granularity(), r.summary());
    }

    /** Gets season summary for a period. */
    @Tool(name = "get_season_summary", description = "Summary for a date range: W/L/P, profit, ROI")
    public SeasonResponse getSeasonSummary(
            @ToolParam(description = "Bankroll ID") String bankrollId,
            @ToolParam(description = "Start date (YYYY-MM-DD)") String startDate,
            @ToolParam(description = "End date (YYYY-MM-DD)") String endDate) {
        SeasonSummaryResult r = analyticsService.getSeasonSummary(bankrollId, startDate, endDate);
        return new SeasonResponse(r.totalBets(), r.wins(), r.losses(), r.pushes(), r.netProfit(), r.roi(), r.summary());
    }

    /** Exports bet history. */
    @Tool(name = "export_bet_history", description = "Export bet history to JSON or CSV format")
    public ExportResponse exportBetHistory(
            @ToolParam(description = "Bankroll ID") String bankrollId,
            @ToolParam(description = "Format: json or csv") String format) {
        ExportResult r = analyticsService.exportBetHistory(bankrollId, format);
        return new ExportResponse(r.data(), r.format(), r.count(), r.summary());
    }
}
