package com.sportspredictor.agent.prompt;

import com.sportspredictor.agent.strategy.BettingStrategy;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Builds system and user prompts for each workflow phase.
 * Strategy rules are injected into system prompts so Claude
 * operates within the configured risk parameters.
 */
@Component
public class AgentPrompts {

    private static final String BASE_SYSTEM_PROMPT =
            """
            You are an autonomous AI sports betting analyst. You make data-driven \
            decisions using the MCP tools available to you. You are placing emulated \
            bets for research and learning purposes — no real money is at risk.

            You must follow these strategy rules strictly:
            %s

            General rules:
            - Always check bankroll status and daily limits before placing bets
            - Never exceed the maximum stake percentage per bet
            - Log your reasoning for each betting decision
            - If no good opportunities exist, it is better to pass than force a bet
            - Consider injuries, weather, trends, and line movement in your analysis
            """;

    public String settlementSystemPrompt(BettingStrategy strategy) {
        return BASE_SYSTEM_PROMPT.formatted(strategy.toPromptRules());
    }

    public String settlementUserPrompt(List<String> sports) {
        return """
                Settle yesterday's completed bets and analyze closing line value.

                For each sport (%s):
                1. Call auto_settle_bets to settle any pending bets with completed games
                2. Call get_closing_line_value_report to assess our betting skill

                Summarize: how many bets settled, W/L/P record, and CLV performance."""
                .formatted(String.join(", ", sports));
    }

    public String scanSystemPrompt(BettingStrategy strategy) {
        return BASE_SYSTEM_PROMPT.formatted(strategy.toPromptRules());
    }

    public String scanUserPrompt(List<String> sports) {
        return """
                Scan for today's betting opportunities across all active sports.

                For each sport (%s):
                1. Call rank_todays_plays with the strategy's minimum confidence and edge thresholds
                2. Identify the top candidates worth deeper analysis

                Return a ranked list of candidates with sport, event, and initial confidence."""
                .formatted(String.join(", ", sports));
    }

    public String analyzeAndBetSystemPrompt(BettingStrategy strategy) {
        return BASE_SYSTEM_PROMPT.formatted(strategy.toPromptRules());
    }

    public String analyzeAndBetUserPrompt(String candidates) {
        return """
                Perform deep analysis on the following candidates and place bets \
                on the ones that meet the strategy criteria.

                Candidates:
                %s

                For each candidate:
                1. Call generate_prediction for the full prediction
                2. Call compare_matchup for side-by-side team comparison
                3. Call analyze_trends for recent performance trends
                4. Call compare_odds_across_books to find the best available line
                5. Call simulate_game for Monte Carlo win probability
                6. Call calculate_expected_value to compute EV
                7. Call calculate_kelly_stake for optimal bet sizing

                Then decide:
                - Check get_bankroll_status and get_daily_limits_status
                - If the bet passes all strategy filters, call place_bet
                - If it doesn't meet criteria, explain why and skip it

                After all candidates are processed, provide a summary of bets placed."""
                .formatted(candidates);
    }

    public String reportSystemPrompt(BettingStrategy strategy) {
        return BASE_SYSTEM_PROMPT.formatted(strategy.toPromptRules());
    }

    public String reportUserPrompt() {
        return """
                Generate the end-of-day performance report.

                1. Call get_bankroll_status for current balance and P/L
                2. Call get_roi_by_category for breakdown by sport and bet type
                3. Call get_streak_history for current win/loss streaks
                4. Call get_daily_performance for today's betting card

                Compile a concise daily report covering:
                - Today's record (W-L-P) and profit/loss
                - Current bankroll balance and overall ROI
                - Best and worst bets of the day
                - Notable streaks or trends
                - Strategy adherence assessment""";
    }
}
