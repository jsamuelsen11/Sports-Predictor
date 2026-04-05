package com.sportspredictor.mcpserver.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers MCP resources for application configuration and model settings. */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ConfigResource {

    private final ObjectMapper objectMapper;

    /** Registers config://model_settings resource. */
    @Bean
    public List<SyncResourceSpecification> configResources() {
        return List.of(modelSettingsResource());
    }

    private SyncResourceSpecification modelSettingsResource() {
        var resource = new McpSchema.Resource(
                "config://model_settings",
                "Model Settings",
                "Model configuration and operating parameters for predictions",
                "application/json",
                null);
        return new SyncResourceSpecification(resource, (exchange, request) -> {
            var settings = Map.of(
                    "minConfidence",
                    0.55,
                    "defaultSport",
                    "nba",
                    "kellyFraction",
                    0.25,
                    "maxParlayLegs",
                    8,
                    "cacheTtlMinutes",
                    5);
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(request.uri(), "application/json", toJson(settings))));
        });
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize resource to JSON: {}", e.getMessage());
            throw new IllegalStateException("Resource serialization failed", e);
        }
    }
}
