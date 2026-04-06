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
public class ScanWorkflow {

    private final ChatClient chatClient;
    private final AgentPrompts prompts;
    private final BettingStrategy strategy;
    private final AgentProperties properties;

    /**
     * Scans all active sports for today's betting opportunities.
     * Returns Claude's ranked list of candidates as a string for
     * the AnalyzeAndBetWorkflow to process.
     */
    public String execute() {
        log.info("Starting scan workflow — strategy: {}, sports: {}", strategy.name(), properties.getSports());

        String candidates = chatClient
                .prompt()
                .system(prompts.scanSystemPrompt(strategy))
                .user(prompts.scanUserPrompt(properties.getSports()))
                .call()
                .content();

        log.info("Scan workflow complete — candidates identified");
        return candidates;
    }
}
