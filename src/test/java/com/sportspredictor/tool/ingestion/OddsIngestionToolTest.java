package com.sportspredictor.tool.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.service.OddsService;
import com.sportspredictor.service.OddsService.EventOdds;
import com.sportspredictor.service.OddsService.HistoricalOddsResult;
import com.sportspredictor.service.OddsService.LiveOddsResult;
import com.sportspredictor.tool.ingestion.OddsIngestionTool.HistoricalOddsResponse;
import com.sportspredictor.tool.ingestion.OddsIngestionTool.LiveOddsResponse;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link OddsIngestionTool}. */
@ExtendWith(MockitoExtension.class)
class OddsIngestionToolTest {

    @Mock
    private OddsService oddsService;

    @InjectMocks
    private OddsIngestionTool oddsIngestionTool;

    // --- Fixture builders ---

    private static EventOdds buildEventOdds(String id, String homeTeam, String awayTeam, int bookmakerCount) {
        List<OddsService.BookmakerOdds> bookmakers = java.util.stream.IntStream.range(0, bookmakerCount)
                .mapToObj(i -> new OddsService.BookmakerOdds("bk" + i, "Bookmaker " + i, List.of()))
                .toList();
        return new EventOdds(id, homeTeam, awayTeam, "2026-01-15T18:00:00Z", bookmakers);
    }

    @Nested
    class GetLiveOdds {

        @Test
        void delegatesToServiceWithAllArguments() {
            when(oddsService.getLiveOdds("nfl", "evt-1", "h2h"))
                    .thenReturn(new LiveOddsResult("nfl", "evt-1", "h2h", List.of(), 0));

            oddsIngestionTool.getLiveOdds("nfl", "evt-1", "h2h");

            verify(oddsService).getLiveOdds("nfl", "evt-1", "h2h");
        }

        @Test
        void buildsSummaryForEmptyResult() {
            when(oddsService.getLiveOdds("nfl", null, null))
                    .thenReturn(new LiveOddsResult("nfl", null, "h2h", List.of(), 0));

            LiveOddsResponse response = oddsIngestionTool.getLiveOdds("nfl", null, null);

            assertThat(response.summary()).contains("No odds found");
            assertThat(response.summary()).contains("nfl");
        }

        @Test
        void buildsSummaryWithEventAndBookmakerCountsForNonEmptyResult() {
            EventOdds event1 = buildEventOdds("e1", "Chiefs", "Ravens", 3);
            EventOdds event2 = buildEventOdds("e2", "Eagles", "49ers", 2);
            when(oddsService.getLiveOdds("nfl", null, "h2h"))
                    .thenReturn(new LiveOddsResult("nfl", null, "h2h", List.of(event1, event2), 2));

            LiveOddsResponse response = oddsIngestionTool.getLiveOdds("nfl", null, "h2h");

            assertThat(response.summary()).contains("2");
            assertThat(response.summary()).contains("NFL");
            assertThat(response.summary()).contains("5"); // total bookmakers: 3 + 2
            assertThat(response.summary()).contains("h2h");
        }

        @Test
        void passesAllServiceResultFieldsThrough() {
            EventOdds event = buildEventOdds("e1", "Chiefs", "Ravens", 1);
            when(oddsService.getLiveOdds("nba", "e1", "spreads"))
                    .thenReturn(new LiveOddsResult("nba", "e1", "spreads", List.of(event), 1));

            LiveOddsResponse response = oddsIngestionTool.getLiveOdds("nba", "e1", "spreads");

            assertThat(response.sport()).isEqualTo("nba");
            assertThat(response.eventId()).isEqualTo("e1");
            assertThat(response.market()).isEqualTo("spreads");
            assertThat(response.events()).hasSize(1);
            assertThat(response.eventCount()).isEqualTo(1);
        }

        @Test
        void summaryIsNonBlank() {
            when(oddsService.getLiveOdds("nfl", null, null))
                    .thenReturn(new LiveOddsResult("nfl", null, "h2h", List.of(), 0));

            LiveOddsResponse response = oddsIngestionTool.getLiveOdds("nfl", null, null);

            assertThat(response.summary()).isNotBlank();
        }
    }

    @Nested
    class GetHistoricalOdds {

        @Test
        void delegatesToServiceWithSportAndDate() {
            when(oddsService.getHistoricalOdds("nfl", "2026-01-10T00:00:00Z"))
                    .thenReturn(new HistoricalOddsResult("nfl", "2026-01-10T00:00:00Z", null, List.of()));

            oddsIngestionTool.getHistoricalOdds("nfl", "2026-01-10T00:00:00Z");

            verify(oddsService).getHistoricalOdds("nfl", "2026-01-10T00:00:00Z");
        }

        @Test
        void buildsSummaryForEmptyResult() {
            when(oddsService.getHistoricalOdds("nfl", "2026-01-10T00:00:00Z"))
                    .thenReturn(new HistoricalOddsResult("nfl", "2026-01-10T00:00:00Z", null, List.of()));

            HistoricalOddsResponse response = oddsIngestionTool.getHistoricalOdds("nfl", "2026-01-10T00:00:00Z");

            assertThat(response.summary()).contains("No historical odds found");
            assertThat(response.summary()).contains("nfl");
            assertThat(response.summary()).contains("2026-01-10T00:00:00Z");
        }

        @Test
        void buildsSummaryWithEventCountForNonEmptyResult() {
            EventOdds event1 = buildEventOdds("e1", "Chiefs", "Ravens", 2);
            EventOdds event2 = buildEventOdds("e2", "Eagles", "49ers", 2);
            String timestamp = "2026-01-10T20:00:00Z";
            when(oddsService.getHistoricalOdds("nfl", "2026-01-10T00:00:00Z"))
                    .thenReturn(new HistoricalOddsResult(
                            "nfl", "2026-01-10T00:00:00Z", timestamp, List.of(event1, event2)));

            HistoricalOddsResponse response = oddsIngestionTool.getHistoricalOdds("nfl", "2026-01-10T00:00:00Z");

            assertThat(response.summary()).contains("2");
            assertThat(response.summary()).contains("NFL");
            assertThat(response.summary()).contains(timestamp);
        }

        @Test
        void passesAllServiceResultFieldsThrough() {
            EventOdds event = buildEventOdds("e1", "Chiefs", "Ravens", 1);
            String timestamp = "2026-01-10T20:00:00Z";
            when(oddsService.getHistoricalOdds("nba", "2026-01-10T00:00:00Z"))
                    .thenReturn(new HistoricalOddsResult("nba", "2026-01-10T00:00:00Z", timestamp, List.of(event)));

            HistoricalOddsResponse response = oddsIngestionTool.getHistoricalOdds("nba", "2026-01-10T00:00:00Z");

            assertThat(response.sport()).isEqualTo("nba");
            assertThat(response.date()).isEqualTo("2026-01-10T00:00:00Z");
            assertThat(response.timestamp()).isEqualTo(timestamp);
            assertThat(response.events()).hasSize(1);
        }
    }
}
