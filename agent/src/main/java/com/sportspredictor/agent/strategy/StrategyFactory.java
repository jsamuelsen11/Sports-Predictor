package com.sportspredictor.agent.strategy;

import com.sportspredictor.agent.config.AgentProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StrategyFactory {

    @Bean
    public BettingStrategy bettingStrategy(AgentProperties properties) {
        return switch (properties.getStrategy().toLowerCase()) {
            case "conservative" -> new ConservativeStrategy();
            case "aggressive" -> new AggressiveStrategy();
            default -> new ModerateStrategy();
        };
    }
}
