package com.sportspredictor.tool;

import java.time.Instant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tool providing a basic health-check endpoint to verify the annotation pipeline. */
@Service
public class HealthCheckTool {

    /** Returns server health status, current timestamp, and an echo of the provided message. */
    @Tool(name = "health_check", description = "Returns server health status and current timestamp")
    public String healthCheck(
            @ToolParam(description = "Optional ping message to echo back", required = false) String message) {
        String echo = (message != null && !message.isBlank()) ? message : "pong";
        return "{\"status\":\"ok\",\"timestamp\":\"" + Instant.now() + "\",\"echo\":\"" + echo + "\"}";
    }
}
