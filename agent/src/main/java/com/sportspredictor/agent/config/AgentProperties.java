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
        private StrategyParams conservative = StrategyParams.conservative();
        private StrategyParams moderate = StrategyParams.moderate();
        private StrategyParams aggressive = StrategyParams.aggressive();
    }

    @Getter
    @Setter
    public static class StrategyParams {
        private double minConfidence;
        private double minEdgePct;
        private String kellyFraction;
        private int maxBetsPerDay;
        private double maxStakePct;
        private List<String> allowedBetTypes;
        private int maxParlayLegs;

        static StrategyParams conservative() {
            var p = new StrategyParams();
            p.minConfidence = 0.70;
            p.minEdgePct = 5.0;
            p.kellyFraction = "quarter";
            p.maxBetsPerDay = 2;
            p.maxStakePct = 1.0;
            p.allowedBetTypes = List.of("MONEYLINE", "SPREAD");
            p.maxParlayLegs = 0;
            return p;
        }

        static StrategyParams moderate() {
            var p = new StrategyParams();
            p.minConfidence = 0.60;
            p.minEdgePct = 3.0;
            p.kellyFraction = "half";
            p.maxBetsPerDay = 5;
            p.maxStakePct = 2.0;
            p.allowedBetTypes = List.of("MONEYLINE", "SPREAD", "TOTAL", "PARLAY");
            p.maxParlayLegs = 3;
            return p;
        }

        static StrategyParams aggressive() {
            var p = new StrategyParams();
            p.minConfidence = 0.50;
            p.minEdgePct = 1.5;
            p.kellyFraction = "full";
            p.maxBetsPerDay = 10;
            p.maxStakePct = 5.0;
            p.allowedBetTypes = List.of(
                    "MONEYLINE", "SPREAD", "TOTAL", "PARLAY", "TEASER", "PLAYER_PROP", "FUTURES", "SGP");
            p.maxParlayLegs = 6;
            return p;
        }
    }

    public StrategyParams getActiveStrategy() {
        return switch (strategy.toLowerCase()) {
            case "conservative" -> strategies.getConservative();
            case "aggressive" -> strategies.getAggressive();
            default -> strategies.getModerate();
        };
    }
}
