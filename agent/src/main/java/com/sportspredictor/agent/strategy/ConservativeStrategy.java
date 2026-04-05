package com.sportspredictor.agent.strategy;

import java.util.List;

public record ConservativeStrategy() implements BettingStrategy {

    @Override
    public String name() {
        return "Conservative";
    }

    @Override
    public double minConfidence() {
        return 0.70;
    }

    @Override
    public double minEdgePct() {
        return 5.0;
    }

    @Override
    public String kellyFraction() {
        return "quarter";
    }

    @Override
    public int maxBetsPerDay() {
        return 2;
    }

    @Override
    public double maxStakePct() {
        return 1.0;
    }

    @Override
    public List<String> allowedBetTypes() {
        return List.of("MONEYLINE", "SPREAD");
    }

    @Override
    public int maxParlayLegs() {
        return 0;
    }
}
