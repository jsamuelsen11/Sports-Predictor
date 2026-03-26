package com.sportspredictor.tool;

import com.sportspredictor.service.PredictionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tool for generating game predictions combining multiple data sources. */
@Service
@RequiredArgsConstructor
public class PredictionTool {

    private final PredictionService predictionService;

    /** Prediction response wrapping the service result. */
    public record PredictionResponse(
            String predictionId,
            String eventId,
            String sport,
            String team1Id,
            String team2Id,
            String predictionType,
            String predictedOutcome,
            double confidence,
            double predictedProbability,
            List<String> keyFactors,
            PredictionService.MatchupSummary matchup,
            PredictionService.TrendSummary trends,
            PredictionService.InjurySummary injuries,
            PredictionService.WeatherSummary weather,
            PredictionService.OddsSummary odds,
            String summary) {}

    /** Generates a full prediction for a game combining stats, trends, odds, injuries, and weather. */
    @Tool(
            name = "generate_prediction",
            description = "Generate a full prediction for a game combining team stats, trends, odds,"
                    + " injuries, and weather data. Returns predicted winner, confidence"
                    + " score, key factors, and recommended bet type.")
    public PredictionResponse generatePrediction(
            @ToolParam(description = "Sport key (e.g., nfl, nba, mlb, nhl)") String sport,
            @ToolParam(description = "Event ID") String eventId,
            @ToolParam(description = "Team 1 ID") String team1Id,
            @ToolParam(description = "Team 2 ID") String team2Id,
            @ToolParam(description = "Prediction type: SPREAD, MONEYLINE, TOTAL, or PROP") String predictionType,
            @ToolParam(description = "Venue latitude for weather data", required = false) Double latitude,
            @ToolParam(description = "Venue longitude for weather data", required = false) Double longitude,
            @ToolParam(description = "Game date in ISO-8601 format (e.g., 2026-01-15)", required = false)
                    String gameDate) {

        PredictionService.PredictionResult result = predictionService.generatePrediction(
                sport, eventId, team1Id, team2Id, predictionType, latitude, longitude, gameDate);

        return new PredictionResponse(
                result.predictionId(),
                result.eventId(),
                result.sport(),
                result.team1Id(),
                result.team2Id(),
                result.predictionType(),
                result.predictedOutcome(),
                result.confidence(),
                result.predictedProbability(),
                result.keyFactors(),
                result.matchup(),
                result.trends(),
                result.injuries(),
                result.weather(),
                result.odds(),
                result.summary());
    }
}
