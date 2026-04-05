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
public class ReportWorkflow {

    private final ChatClient chatClient;
    private final AgentPrompts prompts;
    private final BettingStrategy strategy;

    public String execute() {
        log.info("Starting daily report workflow");

        String report = chatClient
                .prompt()
                .system(prompts.reportSystemPrompt(strategy))
                .user(prompts.reportUserPrompt())
                .call()
                .content();

        log.info("Daily report generated");
        return report;
    }
}
