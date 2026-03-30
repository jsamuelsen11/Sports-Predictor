package com.sportspredictor.prompt;

import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers MCP prompt templates for simulation-based analysis: season outlook and model audit. */
@Configuration
public class SimulationPromptConfig {

    /** Registers simulation and audit prompt templates. */
    @Bean
    public List<SyncPromptSpecification> simulationPrompts() {
        return List.of(seasonOutlookPrompt(), modelAuditPrompt());
    }

    private SyncPromptSpecification seasonOutlookPrompt() {
        var prompt = new McpSchema.Prompt(
                "season_outlook",
                "Mid/end-of-season team analysis with futures assessment",
                List.of(
                        new McpSchema.PromptArgument("sport", "Sport key (e.g., nba, nfl)", false),
                        new McpSchema.PromptArgument("team_id", "Team ID to analyze", true),
                        new McpSchema.PromptArgument("season", "Season year (e.g., 2026)", true)));
        return new SyncPromptSpecification(prompt, (exchange, request) -> {
            String sport = getArg(request.arguments(), "sport", "nba");
            String teamId = getArg(request.arguments(), "team_id", "");
            String season = getArg(request.arguments(), "season", "current");
            return new McpSchema.GetPromptResult(
                    "Season outlook analysis",
                    List.of(new McpSchema.PromptMessage(
                            McpSchema.Role.USER,
                            new McpSchema.TextContent(String.format(
                                    Locale.ROOT,
                                    "Generate a season outlook for %s %s (season: %s)."
                                            + " Steps:"
                                            + " 1) Use simulate_game (Monte Carlo) on upcoming schedule"
                                            + " to project remaining W-L record."
                                            + " 2) Use get_closing_line_value_report to evaluate recent"
                                            + " prediction quality for this team's games."
                                            + " 3) Use get_prediction_confidence_factors on recent"
                                            + " predictions for context."
                                            + " 4) Assess playoff probability, over/under win total,"
                                            + " and any futures value."
                                            + " 5) Produce a structured outlook with projections and"
                                            + " recommended futures bets.",
                                    sport.toUpperCase(Locale.ROOT),
                                    teamId,
                                    season)))));
        });
    }

    private SyncPromptSpecification modelAuditPrompt() {
        var prompt = new McpSchema.Prompt(
                "model_audit",
                "Analyze where the prediction model performs well vs poorly",
                List.of(
                        new McpSchema.PromptArgument("sport", "Sport filter (optional)", true),
                        new McpSchema.PromptArgument("date_range", "Date range (e.g., 2026-01 to 2026-03)", true)));
        return new SyncPromptSpecification(prompt, (exchange, request) -> {
            String sport = getArg(request.arguments(), "sport", "all");
            String dateRange = getArg(request.arguments(), "date_range", "last 30 days");
            return new McpSchema.GetPromptResult(
                    "Model audit report",
                    List.of(new McpSchema.PromptMessage(
                            McpSchema.Role.USER,
                            new McpSchema.TextContent(String.format(
                                    Locale.ROOT,
                                    "Perform a prediction model audit for %s (%s)."
                                            + " Steps:"
                                            + " 1) Use compare_to_random to benchmark vs random and"
                                            + " always-favorites strategies."
                                            + " 2) Use get_closing_line_value_report to assess CLV trend."
                                            + " 3) Use get_prediction_confidence_factors on a sample of"
                                            + " recent predictions."
                                            + " 4) Use get_prediction_accuracy for calibration analysis."
                                            + " 5) Produce an audit report covering:"
                                            + " strengths, weaknesses, sport-specific performance,"
                                            + " bet-type performance, and improvement recommendations.",
                                    sport.toUpperCase(Locale.ROOT),
                                    dateRange)))));
        });
    }

    private static String getArg(Map<String, Object> args, String key, String defaultValue) {
        if (args == null) {
            return defaultValue;
        }
        Object value = args.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
