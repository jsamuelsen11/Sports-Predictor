package com.sportspredictor.agent.strategy;

import java.util.List;

public record AggressiveStrategy() implements BettingStrategy {

    @Override
    public String name() {
        return "Aggressive";
    }

    @Override
    public double minConfidence() {
        return 0.50;
    }

    @Override
    public double minEdgePct() {
        return 1.5;
    }

    @Override
    public String kellyFraction() {
        return "full";
    }

    @Override
    public int maxBetsPerDay() {
        return 10;
    }

    @Override
    public double maxStakePct() {
        return 5.0;
    }

    @Override
    public List<String> allowedBetTypes() {
        return List.of("MONEYLINE", "SPREAD", "TOTAL", "PARLAY", "TEASER", "PLAYER_PROP", "FUTURES", "SGP");
    }

    @Override
    public int maxParlayLegs() {
        return 6;
    }
}
