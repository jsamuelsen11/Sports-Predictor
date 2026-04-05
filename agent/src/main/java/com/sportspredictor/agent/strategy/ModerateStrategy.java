package com.sportspredictor.agent.strategy;

import java.util.List;

public record ModerateStrategy() implements BettingStrategy {

    @Override
    public String name() {
        return "Moderate";
    }

    @Override
    public double minConfidence() {
        return 0.60;
    }

    @Override
    public double minEdgePct() {
        return 3.0;
    }

    @Override
    public String kellyFraction() {
        return "half";
    }

    @Override
    public int maxBetsPerDay() {
        return 5;
    }

    @Override
    public double maxStakePct() {
        return 2.0;
    }

    @Override
    public List<String> allowedBetTypes() {
        return List.of("MONEYLINE", "SPREAD", "TOTAL", "PARLAY");
    }

    @Override
    public int maxParlayLegs() {
        return 3;
    }
}
