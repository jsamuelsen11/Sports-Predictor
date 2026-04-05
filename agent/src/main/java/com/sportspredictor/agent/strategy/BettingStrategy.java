package com.sportspredictor.agent.strategy;

import java.util.List;

/**
 * Sealed interface representing a betting strategy with risk parameters.
 * Each implementation provides preset values that constrain Claude's
 * betting decisions during the agent's daily cycle.
 */
public sealed interface BettingStrategy
        permits ConservativeStrategy, ModerateStrategy, AggressiveStrategy {

    String name();

    double minConfidence();

    double minEdgePct();

    String kellyFraction();

    int maxBetsPerDay();

    double maxStakePct();

    List<String> allowedBetTypes();

    int maxParlayLegs();

    /** Format strategy rules for injection into the LLM system prompt. */
    default String toPromptRules() {
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
                        name(),
                        minConfidence() * 100,
                        minEdgePct(),
                        kellyFraction(),
                        maxBetsPerDay(),
                        maxStakePct(),
                        String.join(", ", allowedBetTypes()),
                        maxParlayLegs());
    }
}
