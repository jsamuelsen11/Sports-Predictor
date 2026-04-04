package com.sportspredictor.mcpserver.prompt;

import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers MCP prompt templates for market analysis: weather, sharp money, props, parlay optimizer. */
@Configuration
public class MarketAnalysisPromptConfig {

    /** Registers market analysis prompt templates. */
    @Bean
    public List<SyncPromptSpecification> marketAnalysisPrompts() {
        return List.of(weatherImpactPrompt(), sharpMoneyPrompt(), propBuilderPrompt(), parlayOptimizerPrompt());
    }

    private SyncPromptSpecification weatherImpactPrompt() {
        var prompt = new McpSchema.Prompt(
                "weather_impact",
                "Assess weather impact on an outdoor game's totals and spread",
                List.of(
                        new McpSchema.PromptArgument("game_id", "Game/event ID", false),
                        new McpSchema.PromptArgument("sport", "Sport key (e.g., nfl, mlb)", false)));
        return new SyncPromptSpecification(prompt, (exchange, request) -> {
            String gameId = getArg(request.arguments(), "game_id", "");
            String sport = getArg(request.arguments(), "sport", "nfl");
            return new McpSchema.GetPromptResult(
                    "Weather impact analysis",
                    List.of(new McpSchema.PromptMessage(
                            McpSchema.Role.USER,
                            new McpSchema.TextContent(String.format(
                                    Locale.ROOT,
                                    "Analyze the weather impact on %s game %s."
                                            + " Steps: 1) Use get_weather_forecast for venue conditions."
                                            + " 2) Use compare_matchup for team stats."
                                            + " 3) Assess how wind, rain, temperature affect totals"
                                            + " and spread. 4) Recommend any weather-based adjustments.",
                                    sport.toUpperCase(Locale.ROOT),
                                    gameId)))));
        });
    }

    private SyncPromptSpecification sharpMoneyPrompt() {
        var prompt = new McpSchema.Prompt(
                "sharp_money_report",
                "Analyze line movement and betting splits to detect sharp action",
                List.of(
                        new McpSchema.PromptArgument("sport", "Sport key", false),
                        new McpSchema.PromptArgument("date", "Date (YYYY-MM-DD)", true)));
        return new SyncPromptSpecification(prompt, (exchange, request) -> {
            String sport = getArg(request.arguments(), "sport", "nba");
            return new McpSchema.GetPromptResult(
                    "Sharp money report",
                    List.of(new McpSchema.PromptMessage(
                            McpSchema.Role.USER,
                            new McpSchema.TextContent(String.format(
                                    Locale.ROOT,
                                    "Generate a sharp money report for %s."
                                            + " Steps: 1) Use detect_line_movement for each game."
                                            + " 2) Use get_market_consensus for true probabilities."
                                            + " 3) Flag games where lines moved against public"
                                            + " betting. 4) Summarize sharp action.",
                                    sport.toUpperCase(Locale.ROOT))))));
        });
    }

    private SyncPromptSpecification propBuilderPrompt() {
        var prompt = new McpSchema.Prompt(
                "prop_builder",
                "Build a player prop bet card for a specific game",
                List.of(
                        new McpSchema.PromptArgument("game_id", "Game/event ID", false),
                        new McpSchema.PromptArgument("sport", "Sport key", false),
                        new McpSchema.PromptArgument("num_props", "Number of props to include (e.g., 5)", true)));
        return new SyncPromptSpecification(prompt, (exchange, request) -> {
            String gameId = getArg(request.arguments(), "game_id", "");
            String sport = getArg(request.arguments(), "sport", "nba");
            String numProps = getArg(request.arguments(), "num_props", "5");
            return new McpSchema.GetPromptResult(
                    "Player prop card",
                    List.of(new McpSchema.PromptMessage(
                            McpSchema.Role.USER,
                            new McpSchema.TextContent(String.format(
                                    Locale.ROOT,
                                    "Build a %s-prop player card for %s game %s."
                                            + " Steps: 1) Use generate_prop_prediction for top"
                                            + " players. 2) Use compare_matchup for context."
                                            + " 3) Present ranked prop picks with confidence"
                                            + " and recommended stake.",
                                    numProps,
                                    sport.toUpperCase(Locale.ROOT),
                                    gameId)))));
        });
    }

    private SyncPromptSpecification parlayOptimizerPrompt() {
        var prompt = new McpSchema.Prompt(
                "parlay_optimizer",
                "Evaluate and optimize a proposed parlay for correlation and value",
                List.of(new McpSchema.PromptArgument("legs", "Description of parlay legs", false)));
        return new SyncPromptSpecification(prompt, (exchange, request) -> {
            String legs = getArg(request.arguments(), "legs", "");
            return new McpSchema.GetPromptResult(
                    "Parlay optimizer",
                    List.of(new McpSchema.PromptMessage(
                            McpSchema.Role.USER,
                            new McpSchema.TextContent(String.format(
                                    Locale.ROOT,
                                    "Optimize this parlay: %s."
                                            + " Steps: 1) Use generate_parlay_analysis to"
                                            + " evaluate correlation. 2) Use get_daily_limits_status"
                                            + " for bankroll constraints."
                                            + " 3) Suggest removing or replacing legs to reduce"
                                            + " correlation. 4) Use calculate_kelly_stake for sizing.",
                                    legs)))));
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
