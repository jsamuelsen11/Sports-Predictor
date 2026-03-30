package com.sportspredictor.tool;

import com.sportspredictor.service.BenchmarkService;
import com.sportspredictor.service.BenchmarkService.BenchmarkResult;
import com.sportspredictor.service.BenchmarkService.StrategyMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tool for benchmarking system performance against random/favorites strategies. */
@Service
@RequiredArgsConstructor
public class BenchmarkTool {

    private final BenchmarkService benchmarkService;

    /** Response for compare_to_random. */
    public record BenchmarkResponse(
            StrategyMetrics systemMetrics,
            StrategyMetrics randomMetrics,
            StrategyMetrics favoritesMetrics,
            String summary) {}

    /** Compares system betting performance vs random and always-favorites strategies. */
    @Tool(
            name = "compare_to_random",
            description = "Compare system betting performance against random betting and"
                    + " always-bet-favorites strategies. Returns side-by-side win rate, P/L,"
                    + " ROI, and max drawdown")
    public BenchmarkResponse compareToRandom(
            @ToolParam(description = "Start date (YYYY-MM-DD)") String startDate,
            @ToolParam(description = "End date (YYYY-MM-DD)") String endDate,
            @ToolParam(description = "Sport filter (optional)", required = false) String sport) {

        BenchmarkResult r = benchmarkService.compareToRandom(startDate, endDate, sport);
        return new BenchmarkResponse(r.systemMetrics(), r.randomMetrics(), r.favoritesMetrics(), r.summary());
    }
}
