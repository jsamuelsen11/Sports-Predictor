package com.sportspredictor.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link HealthCheckTool}. */
class HealthCheckToolTest {

    private final HealthCheckTool tool = new HealthCheckTool();

    @Test
    void returnsOkStatus() {
        String result = tool.healthCheck(null);
        assertThat(result).contains("\"status\":\"ok\"");
    }

    @Test
    void defaultsEchoToPong() {
        String result = tool.healthCheck(null);
        assertThat(result).contains("\"echo\":\"pong\"");
    }

    @Test
    void echoesProvidedMessage() {
        String result = tool.healthCheck("hello");
        assertThat(result).contains("\"echo\":\"hello\"");
    }

    @Test
    void treatsBlankMessageAsDefault() {
        String result = tool.healthCheck("   ");
        assertThat(result).contains("\"echo\":\"pong\"");
    }

    @Test
    void containsTimestamp() {
        String result = tool.healthCheck(null);
        assertThat(result).contains("\"timestamp\":\"");
    }
}
