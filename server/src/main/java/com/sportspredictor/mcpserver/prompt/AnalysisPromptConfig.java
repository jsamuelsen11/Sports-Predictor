package com.sportspredictor.mcpserver.prompt;

import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers MCP prompt templates for sports analysis workflows. */
@Configuration
public class AnalysisPromptConfig {

    /** Registers all analysis prompt templates. */
    @Bean
    public List<SyncPromptSpecification> analysisPrompts() {
        return List.of(
                dailyPicksPrompt(),
                gameBreakdownPrompt(),
                injuryImpactPrompt(),
                valueScanPrompt(),
                matchupHistoryPrompt(),
                bankrollReviewPrompt());
    }

    private SyncPromptSpecification dailyPicksPrompt() {
        var prompt = new McpSchema.Prompt(
                "daily_picks",
                "Generate today's top betting picks ranked by expected value",
                List.of(
                        new McpSchema.PromptArgument("sport", "Sport key (e.g., nfl, nba, mlb, nhl)", true),
                        new McpSchema.PromptArgument(
                                "min_confidence", "Minimum confidence threshold (e.g., 0.6)", false),
                        new McpSchema.PromptArgument("max_picks", "Maximum number of picks to show (e.g., 5)", false)));
        return new SyncPromptSpecification(prompt, (exchange, request) -> {
            String sport = getArg(request.arguments(), "sport", "nba");
            String minConf = getArg(request.arguments(), "min_confidence", "0.55");
            String maxPicks = getArg(request.arguments(), "max_picks", "5");
            return new McpSchema.GetPromptResult(
                    "Today's top picks analysis",
                    List.of(new McpSchema.PromptMessage(
                            McpSchema.Role.USER,
                            new McpSchema.TextContent(String.format(
                                    Locale.ROOT,
                                    "Analyze today's %s games and generate a ranked card of the top %s plays."
                                            + " Steps: 1) Use rank_todays_plays with min_confidence=%s."
                                            + " 2) For the top picks, use compare_matchup for detailed stats."
                                            + " 3) Use analyze_trends for recent form on each team."
                                            + " 4) Use compare_odds_across_books for best lines."
                                            + " 5) Use calculate_kelly_stake for optimal sizing."
                                            + " Present a ranked card with confidence, edge, and recommended"
                                            + " stake for each play.",
                                    sport.toUpperCase(Locale.ROOT),
                                    maxPicks,
                                    minConf)))));
        });
    }

    private SyncPromptSpecification gameBreakdownPrompt() {
        var prompt = new McpSchema.Prompt(
                "game_breakdown",
                "Provide a comprehensive breakdown of a specific game",
                List.of(
                        new McpSchema.PromptArgument("sport", "Sport key (e.g., nfl, nba)", true),
                        new McpSchema.PromptArgument("event_id", "Event ID (optional if teams provided)", false),
                        new McpSchema.PromptArgument("team1", "Team 1 name or ID", false),
                        new McpSchema.PromptArgument("team2", "Team 2 name or ID", false)));
        return new SyncPromptSpecification(prompt, (exchange, request) -> {
            String sport = getArg(request.arguments(), "sport", "nba");
            String team1 = getArg(request.arguments(), "team1", "");
            String team2 = getArg(request.arguments(), "team2", "");
            return new McpSchema.GetPromptResult(
                    "Game breakdown analysis",
                    List.of(new McpSchema.PromptMessage(
                            McpSchema.Role.USER,
                            new McpSchema.TextContent(String.format(
                                    Locale.ROOT,
                                    "Provide a complete breakdown of the %s game between %s and %s."
                                            + " Steps: 1) Use compare_matchup for head-to-head stats."
                                            + " 2) Use analyze_trends for each team's recent form."
                                            + " 3) Use compare_odds_across_books for current lines."
                                            + " 4) Use generate_prediction for a prediction with confidence."
                                            + " 5) Check injuries with get_injuries."
                                            + " Present the analysis with a clear recommendation.",
                                    sport.toUpperCase(Locale.ROOT),
                                    team1,
                                    team2)))));
        });
    }

    private SyncPromptSpecification injuryImpactPrompt() {
        var prompt = new McpSchema.Prompt(
                "injury_impact",
                "Analyze how injuries affect an upcoming game",
                List.of(
                        new McpSchema.PromptArgument("sport", "Sport key", true),
                        new McpSchema.PromptArgument("team", "Team name or ID", true),
                        new McpSchema.PromptArgument("player_name", "Specific player to analyze (optional)", false)));
        return new SyncPromptSpecification(prompt, (exchange, request) -> {
            String sport = getArg(request.arguments(), "sport", "nba");
            String team = getArg(request.arguments(), "team", "");
            String player = getArg(request.arguments(), "player_name", "");
            return new McpSchema.GetPromptResult(
                    "Injury impact analysis",
                    List.of(new McpSchema.PromptMessage(
                            McpSchema.Role.USER,
                            new McpSchema.TextContent(String.format(
                                    Locale.ROOT,
                                    "Analyze the injury situation for %s (%s).%s"
                                            + " Steps: 1) Use get_injuries to see the full injury report."
                                            + " 2) Use analyze_trends to see how the team performs with/without"
                                            + " key players. 3) Use compare_odds_across_books to see if the"
                                            + " line has moved due to injury news."
                                            + " Provide an assessment of how injuries affect the betting line.",
                                    team,
                                    sport.toUpperCase(Locale.ROOT),
                                    player.isEmpty() ? "" : " Focus on " + player + ".")))));
        });
    }

    private SyncPromptSpecification valueScanPrompt() {
        var prompt = new McpSchema.Prompt(
                "value_scan",
                "Scan the market for value betting opportunities",
                List.of(
                        new McpSchema.PromptArgument("sport", "Sport key", true),
                        new McpSchema.PromptArgument("min_ev_percent", "Minimum edge percentage (e.g., 3.0)", false),
                        new McpSchema.PromptArgument(
                                "bankroll_pct", "Percentage of bankroll to risk per play", false)));
        return new SyncPromptSpecification(prompt, (exchange, request) -> {
            String sport = getArg(request.arguments(), "sport", "nba");
            String minEv = getArg(request.arguments(), "min_ev_percent", "3.0");
            String bankrollPct = getArg(request.arguments(), "bankroll_pct", "2.0");
            return new McpSchema.GetPromptResult(
                    "Value scan analysis",
                    List.of(new McpSchema.PromptMessage(
                            McpSchema.Role.USER,
                            new McpSchema.TextContent(String.format(
                                    Locale.ROOT,
                                    "Scan today's %s market for value bets with at least %s%% edge."
                                            + " Steps: 1) Use find_value_bets with min_edge=%s."
                                            + " 2) For each value bet found, use generate_prediction for"
                                            + " confidence. 3) Use calculate_kelly_stake for sizing."
                                            + " Target %s%% of bankroll per play."
                                            + " Present only plays with positive expected value.",
                                    sport.toUpperCase(Locale.ROOT),
                                    minEv,
                                    minEv,
                                    bankrollPct)))));
        });
    }

    private SyncPromptSpecification matchupHistoryPrompt() {
        var prompt = new McpSchema.Prompt(
                "matchup_history",
                "Analyze the historical matchup between two teams",
                List.of(
                        new McpSchema.PromptArgument("sport", "Sport key", true),
                        new McpSchema.PromptArgument("team1", "Team 1 name or ID", true),
                        new McpSchema.PromptArgument("team2", "Team 2 name or ID", true),
                        new McpSchema.PromptArgument("num_games", "Number of recent games to analyze", false)));
        return new SyncPromptSpecification(prompt, (exchange, request) -> {
            String sport = getArg(request.arguments(), "sport", "nba");
            String team1 = getArg(request.arguments(), "team1", "");
            String team2 = getArg(request.arguments(), "team2", "");
            String numGames = getArg(request.arguments(), "num_games", "10");
            return new McpSchema.GetPromptResult(
                    "Matchup history analysis",
                    List.of(new McpSchema.PromptMessage(
                            McpSchema.Role.USER,
                            new McpSchema.TextContent(String.format(
                                    Locale.ROOT,
                                    "Analyze the rivalry between %s and %s in %s over the last %s games."
                                            + " Steps: 1) Use compare_matchup for current season stats."
                                            + " 2) Use analyze_trends for each team's trajectory."
                                            + " 3) Use compare_odds_across_books for current lines."
                                            + " Identify patterns, trends, and which team has the edge.",
                                    team1,
                                    team2,
                                    sport.toUpperCase(Locale.ROOT),
                                    numGames)))));
        });
    }

    private static String getArg(Map<String, Object> arguments, String key, String defaultValue) {
        if (arguments == null || !arguments.containsKey(key)) {
            return defaultValue;
        }
        Object value = arguments.get(key);
        return value != null ? String.valueOf(value) : defaultValue;
    }

    private SyncPromptSpecification bankrollReviewPrompt() {
        var prompt = new McpSchema.Prompt(
                "bankroll_review",
                "Review betting performance and bankroll health",
                List.of(
                        new McpSchema.PromptArgument("sport", "Sport filter (optional)", false),
                        new McpSchema.PromptArgument("time_period", "Time period: 7d, 30d, 90d, or season", false)));
        return new SyncPromptSpecification(prompt, (exchange, request) -> {
            String sport = getArg(request.arguments(), "sport", "all");
            String period = getArg(request.arguments(), "time_period", "30d");
            return new McpSchema.GetPromptResult(
                    "Bankroll review",
                    List.of(new McpSchema.PromptMessage(
                            McpSchema.Role.USER,
                            new McpSchema.TextContent(String.format(
                                    Locale.ROOT,
                                    "Review my betting performance for %s over the last %s."
                                            + " Steps: 1) Use get_prediction_accuracy to see win rate and"
                                            + " accuracy by confidence level."
                                            + " 2) Analyze accuracy by sport and bet type."
                                            + " 3) Identify which confidence levels and bet types are"
                                            + " most profitable."
                                            + " Provide recommendations for improving strategy.",
                                    "all".equals(sport) ? "all sports" : sport.toUpperCase(Locale.ROOT),
                                    period)))));
        });
    }
}
