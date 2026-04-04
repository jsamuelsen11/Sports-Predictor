package com.sportspredictor.mcpserver.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.mcpserver.service.OddsComparisonService;
import com.sportspredictor.mcpserver.service.OddsComparisonService.OddsComparison;
import com.sportspredictor.mcpserver.service.OddsComparisonService.ValueBet;
import com.sportspredictor.mcpserver.tool.OddsComparisonTool.OddsComparisonResponse;
import com.sportspredictor.mcpserver.tool.OddsComparisonTool.ValueBetsResponse;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link OddsComparisonTool}. */
@ExtendWith(MockitoExtension.class)
class OddsComparisonToolTest {

    @Mock
    private OddsComparisonService oddsComparisonService;

    @InjectMocks
    private OddsComparisonTool oddsComparisonTool;

    /** Tests for {@link OddsComparisonTool#compareOddsAcrossBooks}. */
    @Nested
    class CompareOddsAcrossBooks {

        @Test
        void delegatesToService() {
            var comparison = new OddsComparison("evt-1", "Team A", "Team B", List.of(), null, null, "Summary");
            when(oddsComparisonService.compareOdds("nba", "evt-1", "h2h")).thenReturn(comparison);

            oddsComparisonTool.compareOddsAcrossBooks("nba", "evt-1", "h2h");

            verify(oddsComparisonService).compareOdds("nba", "evt-1", "h2h");
        }

        @Test
        void returnsSummary() {
            var comparison = new OddsComparison("evt-1", "Team A", "Team B", List.of(), null, null, "Test summary");
            when(oddsComparisonService.compareOdds("nba", "evt-1", null)).thenReturn(comparison);

            OddsComparisonResponse response = oddsComparisonTool.compareOddsAcrossBooks("nba", "evt-1", null);

            assertThat(response.summary()).isEqualTo("Test summary");
        }
    }

    /** Tests for {@link OddsComparisonTool#findValueBets}. */
    @Nested
    class FindValueBets {

        @Test
        void delegatesToService() {
            when(oddsComparisonService.findValueBets("nba", "h2h", 3.0)).thenReturn(List.of());

            oddsComparisonTool.findValueBets("nba", 3.0, "h2h");

            verify(oddsComparisonService).findValueBets("nba", "h2h", 3.0);
        }

        @Test
        void returnsTotalFoundCount() {
            var bet = new ValueBet("evt-1", "Team A", "Team B", "Team A", "DK", -110, 0.52, 5.0, 0.1, "GOOD");
            when(oddsComparisonService.findValueBets("nba", null, 2.0)).thenReturn(List.of(bet));

            ValueBetsResponse response = oddsComparisonTool.findValueBets("nba", 2.0, null);

            assertThat(response.totalFound()).isEqualTo(1);
            assertThat(response.valueBets()).hasSize(1);
        }

        @Test
        void summaryContainsSport() {
            when(oddsComparisonService.findValueBets("nfl", null, 5.0)).thenReturn(List.of());

            ValueBetsResponse response = oddsComparisonTool.findValueBets("nfl", 5.0, null);

            assertThat(response.summary()).contains("nfl");
        }
    }
}
