package com.sportspredictor.mcpserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sportspredictor.mcpserver.service.OddsComparisonService.OddsComparison;
import com.sportspredictor.mcpserver.service.OddsComparisonService.ValueBet;
import com.sportspredictor.mcpserver.service.OddsService.BookmakerOdds;
import com.sportspredictor.mcpserver.service.OddsService.EventOdds;
import com.sportspredictor.mcpserver.service.OddsService.LiveOddsResult;
import com.sportspredictor.mcpserver.service.OddsService.MarketOdds;
import com.sportspredictor.mcpserver.service.OddsService.OutcomeOdds;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link OddsComparisonService}. */
@ExtendWith(MockitoExtension.class)
class OddsComparisonServiceTest {

    @Mock
    private OddsService oddsService;

    @InjectMocks
    private OddsComparisonService oddsComparisonService;

    private static LiveOddsResult buildLiveOdds(String eventId) {
        var outcome1 = new OutcomeOdds("Team A", -110.0, null);
        var outcome2 = new OutcomeOdds("Team B", 100.0, null);
        var market = new MarketOdds("h2h", List.of(outcome1, outcome2));
        var book1 = new BookmakerOdds("dk", "DraftKings", List.of(market));

        var outcome3 = new OutcomeOdds("Team A", -105.0, null);
        var outcome4 = new OutcomeOdds("Team B", -105.0, null);
        var market2 = new MarketOdds("h2h", List.of(outcome3, outcome4));
        var book2 = new BookmakerOdds("fd", "FanDuel", List.of(market2));

        var event = new EventOdds(eventId, "Team A", "Team B", "2026-01-15T21:00:00Z", List.of(book1, book2));
        return new LiveOddsResult("nba", eventId, "h2h", List.of(event), 1);
    }

    /** Tests for {@link OddsComparisonService#compareOdds}. */
    @Nested
    class CompareOdds {

        @Test
        void returnsBookmakerLines() {
            when(oddsService.getLiveOdds("nba", "evt-1", "h2h")).thenReturn(buildLiveOdds("evt-1"));

            OddsComparison result = oddsComparisonService.compareOdds("nba", "evt-1", "h2h");

            assertThat(result.bookmakerLines()).hasSize(4);
        }

        @Test
        void findsBestHomeLineAsMostPositive() {
            when(oddsService.getLiveOdds("nba", "evt-1", "h2h")).thenReturn(buildLiveOdds("evt-1"));

            OddsComparison result = oddsComparisonService.compareOdds("nba", "evt-1", "h2h");

            assertThat(result.bestHome()).isNotNull();
            assertThat(result.bestHome().bookmaker()).isEqualTo("FanDuel");
            assertThat(result.bestHome().price()).isEqualTo(-105);
        }

        @Test
        void findsBestAwayLine() {
            when(oddsService.getLiveOdds("nba", "evt-1", "h2h")).thenReturn(buildLiveOdds("evt-1"));

            OddsComparison result = oddsComparisonService.compareOdds("nba", "evt-1", "h2h");

            assertThat(result.bestAway()).isNotNull();
            assertThat(result.bestAway().bookmaker()).isEqualTo("DraftKings");
            assertThat(result.bestAway().price()).isEqualTo(100);
        }

        @Test
        void handlesEmptyEvents() {
            var emptyOdds = new LiveOddsResult("nba", "evt-1", "h2h", List.of(), 0);
            when(oddsService.getLiveOdds("nba", "evt-1", "h2h")).thenReturn(emptyOdds);

            OddsComparison result = oddsComparisonService.compareOdds("nba", "evt-1", "h2h");

            assertThat(result.bookmakerLines()).isEmpty();
            assertThat(result.bestHome()).isNull();
            assertThat(result.bestAway()).isNull();
        }

        @Test
        void summaryContainsTeamNames() {
            when(oddsService.getLiveOdds("nba", "evt-1", "h2h")).thenReturn(buildLiveOdds("evt-1"));

            OddsComparison result = oddsComparisonService.compareOdds("nba", "evt-1", "h2h");

            assertThat(result.summary()).contains("Team A");
            assertThat(result.summary()).contains("Team B");
        }
    }

    /** Tests for {@link OddsComparisonService#findValueBets}. */
    @Nested
    class FindValueBets {

        @Test
        void findsValueBetsAboveThreshold() {
            when(oddsService.getLiveOdds("nba", null, "h2h")).thenReturn(buildLiveOdds("evt-1"));

            List<ValueBet> valueBets = oddsComparisonService.findValueBets("nba", "h2h", 0.1);

            assertThat(valueBets).isNotEmpty();
        }

        @Test
        void noValueBetsWhenThresholdTooHigh() {
            when(oddsService.getLiveOdds("nba", null, "h2h")).thenReturn(buildLiveOdds("evt-1"));

            List<ValueBet> valueBets = oddsComparisonService.findValueBets("nba", "h2h", 50.0);

            assertThat(valueBets).isEmpty();
        }

        @Test
        void sortsByEdgeDescending() {
            when(oddsService.getLiveOdds("nba", null, "h2h")).thenReturn(buildLiveOdds("evt-1"));

            List<ValueBet> valueBets = oddsComparisonService.findValueBets("nba", "h2h", 0.1);

            if (valueBets.size() > 1) {
                for (int i = 0; i < valueBets.size() - 1; i++) {
                    assertThat(valueBets.get(i).edgePercent())
                            .isGreaterThanOrEqualTo(valueBets.get(i + 1).edgePercent());
                }
            }
        }

        @Test
        void handlesEmptyOdds() {
            var emptyOdds = new LiveOddsResult("nba", null, "h2h", List.of(), 0);
            when(oddsService.getLiveOdds("nba", null, "h2h")).thenReturn(emptyOdds);

            List<ValueBet> valueBets = oddsComparisonService.findValueBets("nba", "h2h", 1.0);

            assertThat(valueBets).isEmpty();
        }
    }
}
