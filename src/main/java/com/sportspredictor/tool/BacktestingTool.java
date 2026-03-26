package com.sportspredictor.tool;

import com.sportspredictor.service.BacktestingService;
import com.sportspredictor.service.BacktestingService.BacktestResult;
import com.sportspredictor.service.BacktestingService.ExportPredictionResult;
import com.sportspredictor.service.BacktestingService.FactorPerformanceResult;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tools for backtesting strategies and model performance analysis. */
@Service
@RequiredArgsConstructor
public class BacktestingTool {

    private final BacktestingService backtestingService;

    /** Response records. */
    public record BacktestResponse(
            int totalPredictions,
            int correct,
            double accuracy,
            double simulatedProfit,
            double roi,
            double maxDrawdown,
            String summary) {}

    /** Factor performance response. */
    public record FactorResponse(Map<String, Double> accuracyByFactor, int total, String factor, String summary) {}

    /** Export response. */
    public record ExportResponse(String data, String format, int count, String summary) {}

    /** Backtests a strategy against historical data. */
    @Tool(
            name = "backtest_strategy",
            description = "Apply betting rules to historical predictions and return simulated P/L")
    public BacktestResponse backtestStrategy(
            @ToolParam(description = "Sport filter (optional)", required = false) String sport,
            @ToolParam(description = "Bet type filter (optional)", required = false) String betType,
            @ToolParam(description = "Min confidence threshold (0.0-1.0)") double minConfidence,
            @ToolParam(description = "Min edge threshold") double minEdge,
            @ToolParam(description = "Start date (YYYY-MM-DD)") String startDate,
            @ToolParam(description = "End date (YYYY-MM-DD)") String endDate) {
        BacktestResult r =
                backtestingService.backtestStrategy(sport, betType, minConfidence, minEdge, startDate, endDate);
        return new BacktestResponse(
                r.totalPredictions(),
                r.correctPredictions(),
                r.accuracy(),
                r.simulatedProfit(),
                r.roi(),
                r.maxDrawdown(),
                r.summary());
    }

    /** Gets model performance breakdown by factor. */
    @Tool(name = "get_model_performance_by_factor", description = "Accuracy breakdown by sport, type, or other factor")
    public FactorResponse getModelPerformanceByFactor(
            @ToolParam(description = "Factor to group by: sport, type") String factor) {
        FactorPerformanceResult r = backtestingService.getModelPerformanceByFactor(factor);
        return new FactorResponse(r.accuracyByFactor(), r.totalPredictions(), r.factor(), r.summary());
    }

    /** Exports prediction log. */
    @Tool(name = "export_prediction_log", description = "Export all predictions with outcomes to CSV or JSON")
    public ExportResponse exportPredictionLog(
            @ToolParam(description = "Sport filter (optional)", required = false) String sport,
            @ToolParam(description = "Format: json or csv") String format) {
        ExportPredictionResult r = backtestingService.exportPredictionLog(sport, format);
        return new ExportResponse(r.data(), r.format(), r.count(), r.summary());
    }
}
