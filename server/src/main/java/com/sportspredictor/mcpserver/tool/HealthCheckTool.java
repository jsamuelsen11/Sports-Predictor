package com.sportspredictor.mcpserver.tool;

import java.time.Instant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tool providing a basic health-check endpoint to verify the annotation pipeline. */
@Service
public class HealthCheckTool {

    /** Health check response containing server status, timestamp, and echo message. */
    public record HealthCheckResponse(String status, Instant timestamp, String echo) {}

    /** Returns server health status, current timestamp, and an echo of the provided message. */
    @Tool(name = "health_check", description = "Returns server health status and current timestamp")
    public HealthCheckResponse healthCheck(
            @ToolParam(description = "Optional ping message to echo back", required = false) String message) {
        String echo = (message != null && !message.isBlank()) ? message : "pong";
        return new HealthCheckResponse("ok", Instant.now(), echo);
    }
}
