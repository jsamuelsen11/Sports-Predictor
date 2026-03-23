package com.sportspredictor.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportspredictor.tool.HealthCheckTool.HealthCheckResponse;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link HealthCheckTool}. */
class HealthCheckToolTest {

    private final HealthCheckTool tool = new HealthCheckTool();

    @Test
    void returnsOkStatus() {
        HealthCheckResponse result = tool.healthCheck(null);
        assertThat(result.status()).isEqualTo("ok");
    }

    @Test
    void defaultsEchoToPong() {
        HealthCheckResponse result = tool.healthCheck(null);
        assertThat(result.echo()).isEqualTo("pong");
    }

    @Test
    void echoesProvidedMessage() {
        HealthCheckResponse result = tool.healthCheck("hello");
        assertThat(result.echo()).isEqualTo("hello");
    }

    @Test
    void preservesQuotesInMessage() {
        HealthCheckResponse result = tool.healthCheck("\"hello\"");
        assertThat(result.echo()).isEqualTo("\"hello\"");
    }

    @Test
    void preservesBackslashesInMessage() {
        HealthCheckResponse result = tool.healthCheck("hello\\world");
        assertThat(result.echo()).isEqualTo("hello\\world");
    }

    @Test
    void preservesNewlinesInMessage() {
        HealthCheckResponse result = tool.healthCheck("line1\nline2");
        assertThat(result.echo()).isEqualTo("line1\nline2");
    }

    @Test
    void treatsBlankMessageAsDefault() {
        HealthCheckResponse result = tool.healthCheck("   ");
        assertThat(result.echo()).isEqualTo("pong");
    }

    @Test
    void containsTimestamp() {
        HealthCheckResponse result = tool.healthCheck(null);
        assertThat(result.timestamp()).isNotNull();
    }
}
