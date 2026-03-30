package com.sportspredictor.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers MCP resources for prediction data. */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class PredictionResource {

    private final ObjectMapper objectMapper;

    /** Registers predictions://today resource. */
    @Bean
    public List<SyncResourceSpecification> predictionResources() {
        return List.of(todayPredictionsResource());
    }

    private SyncResourceSpecification todayPredictionsResource() {
        var resource = new McpSchema.Resource(
                "predictions://today",
                "Today's Predictions",
                "Cached predictions for today's games with confidence scores",
                "application/json",
                null);
        return new SyncResourceSpecification(resource, (exchange, request) -> {
            var data = java.util.Map.of("message", "Today's predictions - use generate_prediction for live data");
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(request.uri(), "application/json", toJson(data))));
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
