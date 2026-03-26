package com.sportspredictor.tool;

import com.sportspredictor.service.PredictionAccuracyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tool for analyzing prediction track record and accuracy metrics. */
@Service
@RequiredArgsConstructor
public class PredictionAccuracyTool {

    private final PredictionAccuracyService predictionAccuracyService;

    /** Prediction accuracy response wrapping the service result. */
    public record AccuracyResponse(
            int totalPredictions,
            int wins,
            int losses,
            double winRate,
            List<PredictionAccuracyService.ConfidenceBucket> confidenceBuckets,
            List<PredictionAccuracyService.SportBreakdown> sportBreakdowns,
            List<PredictionAccuracyService.TypeBreakdown> typeBreakdowns,
            String summary) {}

    /** Analyzes prediction track record with optional filtering. */
    @Tool(
            name = "get_prediction_accuracy",
            description = "Analyze prediction track record showing win rate, accuracy by confidence level,"
                    + " breakdowns by sport and bet type. Only includes settled predictions"
                    + " with known outcomes.")
    public AccuracyResponse getPredictionAccuracy(
            @ToolParam(description = "Filter by sport (e.g., nfl, nba)", required = false) String sport,
            @ToolParam(description = "Filter by prediction type (SPREAD, MONEYLINE, TOTAL, PROP)", required = false)
                    String predictionType,
            @ToolParam(description = "Date range: 7d, 30d, 90d, or season", required = false) String dateRange,
            @ToolParam(description = "Minimum confidence threshold (0.0-1.0)", required = false) Double minConfidence) {

        PredictionAccuracyService.AccuracyResult result =
                predictionAccuracyService.getAccuracy(sport, predictionType, dateRange, minConfidence);

        return new AccuracyResponse(
                result.totalPredictions(),
                result.wins(),
                result.losses(),
                result.winRate(),
                result.confidenceBuckets(),
                result.sportBreakdowns(),
                result.typeBreakdowns(),
                result.summary());
    }
}
