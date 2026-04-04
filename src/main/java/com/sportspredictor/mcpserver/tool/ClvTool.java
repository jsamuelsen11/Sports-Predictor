package com.sportspredictor.mcpserver.tool;

import com.sportspredictor.mcpserver.service.ClvService;
import com.sportspredictor.mcpserver.service.ClvService.ClvDataPoint;
import com.sportspredictor.mcpserver.service.ClvService.ClvHistoryResult;
import com.sportspredictor.mcpserver.service.ClvService.ClvReportResult;
import com.sportspredictor.mcpserver.service.ClvService.SingleClvResult;
import com.sportspredictor.mcpserver.service.ClvService.SportClv;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tools for Closing Line Value (CLV) analysis. */
@Service
@RequiredArgsConstructor
public class ClvTool {

    private final ClvService clvService;

    /** Response for calculate_closing_line_value. */
    public record CalculateClvResponse(
            String betId, int placedOdds, int closingOdds, double clvPercent, String summary) {}

    /** Response for get_closing_line_value_report. */
    public record ClvReportResponse(
            int totalBets, double averageClv, double positiveClvRate, List<SportClv> bySport, String summary) {}

    /** Response for compare_to_closing_line. */
    public record ClvHistoryResponse(List<ClvDataPoint> dataPoints, double trendSlope, String summary) {}

    /** Calculates CLV for a single bet by comparing placed odds vs closing line. */
    @Tool(
            name = "calculate_closing_line_value",
            description = "Calculate Closing Line Value (CLV) for a single bet."
                    + " Compares odds at placement vs closing line — positive CLV indicates skill")
    public CalculateClvResponse calculateClosingLineValue(@ToolParam(description = "Bet ID to analyze") String betId) {
        SingleClvResult r = clvService.calculateClosingLineValue(betId);
        return new CalculateClvResponse(r.betId(), r.placedOdds(), r.closingOdds(), r.clvPercent(), r.summary());
    }

    /** Generates a CLV report across all settled bets. */
    @Tool(
            name = "get_closing_line_value_report",
            description = "Generate a CLV report across settled bets."
                    + " Positive average CLV = skilled bettor beating the closing line")
    public ClvReportResponse getClosingLineValueReport(
            @ToolParam(description = "Start date (YYYY-MM-DD)") String startDate,
            @ToolParam(description = "End date (YYYY-MM-DD)") String endDate,
            @ToolParam(description = "Sport filter (optional)", required = false) String sport) {
        ClvReportResult r = clvService.getClosingLineValueReport(startDate, endDate, sport);
        return new ClvReportResponse(r.totalBets(), r.averageClv(), r.positiveClvRate(), r.bySport(), r.summary());
    }

    /** Analyzes CLV trends over time. */
    @Tool(
            name = "compare_to_closing_line",
            description =
                    "Analyze CLV trends over time to assess whether" + " prediction quality is improving or declining")
    public ClvHistoryResponse compareToClosingLine(
            @ToolParam(description = "Start date (YYYY-MM-DD)") String startDate,
            @ToolParam(description = "End date (YYYY-MM-DD)") String endDate,
            @ToolParam(description = "Sport filter (optional)", required = false) String sport) {
        ClvHistoryResult r = clvService.compareToClosingLine(startDate, endDate, sport);
        return new ClvHistoryResponse(r.dataPoints(), r.trendSlope(), r.summary());
    }
}
