package com.sportspredictor.agent.workflow;

import com.sportspredictor.agent.config.AgentProperties;
import com.sportspredictor.agent.prompt.AgentPrompts;
import com.sportspredictor.agent.strategy.BettingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementWorkflow {

    private final ChatClient chatClient;
    private final AgentPrompts prompts;
    private final BettingStrategy strategy;
    private final AgentProperties properties;

    public String execute() {
        log.info("Starting settlement workflow for sports: {}", properties.getSports());

        String response = chatClient
                .prompt()
                .system(prompts.settlementSystemPrompt(strategy))
                .user(prompts.settlementUserPrompt(properties.getSports()))
                .call()
                .content();

        log.info("Settlement workflow complete");
        return response;
    }
}
