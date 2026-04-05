package com.sportspredictor.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel, ToolCallbackProvider mcpTools) {
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(mcpTools)
                .build();
    }
}
