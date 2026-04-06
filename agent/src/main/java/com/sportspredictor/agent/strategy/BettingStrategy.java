package com.sportspredictor.agent.strategy;

import com.sportspredictor.agent.config.AgentProperties.StrategyParams;
import java.util.List;

/**
 * Immutable view of the active betting strategy, built from configuration.
 * There is a single source of truth for all parameters: {@link StrategyParams}
 * bound from application.yml via {@link com.sportspredictor.agent.config.AgentProperties}.
 */
public record BettingStrategy(
        String name,
        double minConfidence,
        double minEdgePct,
        String kellyFraction,
        int maxBetsPerDay,
        double maxStakePct,
        List<String> allowedBetTypes,
        int maxParlayLegs) {

    public static BettingStrategy from(String name, StrategyParams params) {
        return new BettingStrategy(
                name,
                params.getMinConfidence(),
                params.getMinEdgePct(),
                params.getKellyFraction(),
                params.getMaxBetsPerDay(),
                params.getMaxStakePct(),
                List.copyOf(params.getAllowedBetTypes()),
                params.getMaxParlayLegs());
    }

    /** Format strategy rules for injection into the LLM system prompt. */
    public String toPromptRules() {
        return """
                Strategy: %s
                - Only bet when prediction confidence >= %.0f%%
                - Only bet when expected edge >= %.1f%%
                - Use %s Kelly criterion for stake sizing
                - Maximum %d bets per day
                - Maximum %.1f%% of bankroll per single bet
                - Allowed bet types: %s
                - Maximum parlay legs: %d (0 = no parlays)"""
                .formatted(
                        name,
                        minConfidence * 100,
                        minEdgePct,
                        kellyFraction,
                        maxBetsPerDay,
                        maxStakePct,
                        String.join(", ", allowedBetTypes),
                        maxParlayLegs);
    }
}
