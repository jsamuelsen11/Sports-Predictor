package com.sportspredictor.agent.strategy;

import com.sportspredictor.agent.config.AgentProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StrategyFactory {

    @Bean
    public BettingStrategy bettingStrategy(AgentProperties properties) {
        String name = properties.getStrategy().substring(0, 1).toUpperCase()
                + properties.getStrategy().substring(1).toLowerCase();
        return BettingStrategy.from(name, properties.getActiveStrategyParams());
    }
}
