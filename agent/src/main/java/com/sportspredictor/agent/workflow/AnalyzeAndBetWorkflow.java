package com.sportspredictor.agent.workflow;

import com.sportspredictor.agent.prompt.AgentPrompts;
import com.sportspredictor.agent.strategy.BettingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzeAndBetWorkflow {

    private final ChatClient chatClient;
    private final AgentPrompts prompts;
    private final BettingStrategy strategy;

    /**
     * Performs deep analysis on scan candidates and places bets
     * on opportunities that pass the strategy filters.
     *
     * @param candidates the ranked candidate list from ScanWorkflow
     * @return summary of bets placed and decisions made
     */
    public String execute(String candidates) {
        if (candidates == null || candidates.isBlank()) {
            log.info("No candidates to analyze — skipping bet placement");
            return "No candidates identified. No bets placed.";
        }

        log.info("Starting analysis and bet placement — strategy: {}", strategy.name());

        String result = chatClient
                .prompt()
                .system(prompts.analyzeAndBetSystemPrompt(strategy))
                .user(prompts.analyzeAndBetUserPrompt(candidates))
                .call()
                .content();

        log.info("Analyze and bet workflow complete");
        return result;
    }
}
