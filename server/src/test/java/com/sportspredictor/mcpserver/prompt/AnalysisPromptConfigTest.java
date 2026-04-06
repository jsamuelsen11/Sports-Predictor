package com.sportspredictor.mcpserver.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link AnalysisPromptConfig}. */
class AnalysisPromptConfigTest {

    private final AnalysisPromptConfig config = new AnalysisPromptConfig();

    /** Tests for prompt registration. */
    @Nested
    class PromptRegistration {

        @Test
        void registersSixPrompts() {
            List<SyncPromptSpecification> prompts = config.analysisPrompts();
            assertThat(prompts).hasSize(6);
        }

        @Test
        void dailyPicksPromptExists() {
            List<SyncPromptSpecification> prompts = config.analysisPrompts();
            assertThat(prompts.stream().map(p -> p.prompt().name())).contains("daily_picks");
        }

        @Test
        void gameBreakdownPromptExists() {
            List<SyncPromptSpecification> prompts = config.analysisPrompts();
            assertThat(prompts.stream().map(p -> p.prompt().name())).contains("game_breakdown");
        }

        @Test
        void injuryImpactPromptExists() {
            List<SyncPromptSpecification> prompts = config.analysisPrompts();
            assertThat(prompts.stream().map(p -> p.prompt().name())).contains("injury_impact");
        }

        @Test
        void valueScanPromptExists() {
            List<SyncPromptSpecification> prompts = config.analysisPrompts();
            assertThat(prompts.stream().map(p -> p.prompt().name())).contains("value_scan");
        }

        @Test
        void matchupHistoryPromptExists() {
            List<SyncPromptSpecification> prompts = config.analysisPrompts();
            assertThat(prompts.stream().map(p -> p.prompt().name())).contains("matchup_history");
        }

        @Test
        void bankrollReviewPromptExists() {
            List<SyncPromptSpecification> prompts = config.analysisPrompts();
            assertThat(prompts.stream().map(p -> p.prompt().name())).contains("bankroll_review");
        }
    }

    /** Tests for prompt handler invocation. */
    @Nested
    class PromptHandlers {

        @Test
        void dailyPicksReturnsPromptResult() {
            List<SyncPromptSpecification> prompts = config.analysisPrompts();
            SyncPromptSpecification dailyPicks = prompts.stream()
                    .filter(p -> "daily_picks".equals(p.prompt().name()))
                    .findFirst()
                    .orElseThrow();

            var request = new McpSchema.GetPromptRequest("daily_picks", Map.of("sport", "nba"));
            McpSchema.GetPromptResult result = dailyPicks.promptHandler().apply(null, request);

            assertThat(result.description()).isNotBlank();
            assertThat(result.messages()).hasSize(1);
            assertThat(result.messages().get(0).role()).isEqualTo(McpSchema.Role.USER);
        }

        @Test
        void gameBreakdownReturnsPromptResult() {
            List<SyncPromptSpecification> prompts = config.analysisPrompts();
            SyncPromptSpecification prompt = prompts.stream()
                    .filter(p -> "game_breakdown".equals(p.prompt().name()))
                    .findFirst()
                    .orElseThrow();

            var request = new McpSchema.GetPromptRequest(
                    "game_breakdown", Map.of("sport", "nfl", "team1", "KC", "team2", "SF"));
            McpSchema.GetPromptResult result = prompt.promptHandler().apply(null, request);

            assertThat(result.messages()).hasSize(1);
        }

        @Test
        void bankrollReviewWithDefaults() {
            List<SyncPromptSpecification> prompts = config.analysisPrompts();
            SyncPromptSpecification prompt = prompts.stream()
                    .filter(p -> "bankroll_review".equals(p.prompt().name()))
                    .findFirst()
                    .orElseThrow();

            var request = new McpSchema.GetPromptRequest("bankroll_review", Map.of());
            McpSchema.GetPromptResult result = prompt.promptHandler().apply(null, request);

            assertThat(result.messages()).hasSize(1);
            assertThat(result.description()).isNotBlank();
        }
    }
}
