package com.sportspredictor.agent.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent")
@Getter
@Setter
public class AgentProperties {

    private String strategy = "moderate";
    private List<String> sports = List.of("nba", "nfl", "mlb", "nhl");
    private Schedule schedule = new Schedule();
    private Strategies strategies = new Strategies();

    @Getter
    @Setter
    public static class Schedule {
        private String settleCron = "0 0 8 * * *";
        private String scanCron = "0 0 10 * * *";
        private String reportCron = "0 0 22 * * *";
    }

    @Getter
    @Setter
    public static class Strategies {
        private StrategyParams conservative = new StrategyParams();
        private StrategyParams moderate = new StrategyParams();
        private StrategyParams aggressive = new StrategyParams();
    }

    @Getter
    @Setter
    public static class StrategyParams {
        private double minConfidence = 0.60;
        private double minEdgePct = 3.0;
        private String kellyFraction = "half";
        private int maxBetsPerDay = 5;
        private double maxStakePct = 2.0;
        private List<String> allowedBetTypes = List.of("MONEYLINE", "SPREAD", "TOTAL", "PARLAY");
        private int maxParlayLegs = 3;
    }

    /** Returns the StrategyParams for the currently active strategy. */
    public StrategyParams getActiveStrategyParams() {
        return switch (strategy.toLowerCase()) {
            case "conservative" -> strategies.getConservative();
            case "aggressive" -> strategies.getAggressive();
            default -> strategies.getModerate();
        };
    }
}
