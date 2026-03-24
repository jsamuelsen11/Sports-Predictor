package com.sportspredictor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.client.oddsapi.OddsApiClient;
import com.sportspredictor.client.oddsapi.OddsHistoryResponse;
import com.sportspredictor.client.oddsapi.OddsResponse;
import com.sportspredictor.service.OddsService.HistoricalOddsResult;
import com.sportspredictor.service.OddsService.LiveOddsResult;
import com.sportspredictor.service.SportLeagueMapping.LeagueInfo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link OddsService}. */
@ExtendWith(MockitoExtension.class)
class OddsServiceTest {

    @Mock
    private OddsApiClient oddsApiClient;

    @Mock
    private SportLeagueMapping sportLeagueMapping;

    @InjectMocks
    private OddsService oddsService;

    private static final LeagueInfo NFL_INFO = new LeagueInfo("nfl", "football", "nfl", "americanfootball_nfl", "NFL");

    private static OddsResponse buildOddsResponse(String id, String homeTeam, String awayTeam) {
        OddsResponse.Outcome homeOutcome = new OddsResponse.Outcome(homeTeam, -110, null);
        OddsResponse.Outcome awayOutcome = new OddsResponse.Outcome(awayTeam, -110, null);
        OddsResponse.Market market = new OddsResponse.Market("h2h", List.of(homeOutcome, awayOutcome));
        OddsResponse.Bookmaker bookmaker = new OddsResponse.Bookmaker("draftkings", "DraftKings", List.of(market));
        return new OddsResponse(
                id, "americanfootball_nfl", "NFL", "2026-01-15T18:00:00Z", homeTeam, awayTeam, List.of(bookmaker));
    }

    @BeforeEach
    void setupMapping() {
        when(sportLeagueMapping.resolve("nfl")).thenReturn(NFL_INFO);
    }

    @Nested
    class GetLiveOdds {

        @Test
        void delegatesToClientWithCorrectOddsApiKey() {
            when(oddsApiClient.getOdds(eq("americanfootball_nfl"), eq("us"), eq("h2h"), eq("american")))
                    .thenReturn(List.of());

            oddsService.getLiveOdds("nfl", null, null);

            verify(oddsApiClient).getOdds("americanfootball_nfl", "us", "h2h", "american");
        }

        @Test
        void returnsAllEventsWhenEventIdIsNull() {
            OddsResponse event1 = buildOddsResponse("evt-1", "Chiefs", "Ravens");
            OddsResponse event2 = buildOddsResponse("evt-2", "Eagles", "49ers");
            when(oddsApiClient.getOdds(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(List.of(event1, event2));

            LiveOddsResult result = oddsService.getLiveOdds("nfl", null, "h2h");

            assertThat(result.events()).hasSize(2);
            assertThat(result.eventCount()).isEqualTo(2);
            assertThat(result.sport()).isEqualTo("nfl");
            assertThat(result.market()).isEqualTo("h2h");
        }

        @Test
        void filtersEventsByEventId() {
            OddsResponse event1 = buildOddsResponse("evt-1", "Chiefs", "Ravens");
            OddsResponse event2 = buildOddsResponse("evt-2", "Eagles", "49ers");
            when(oddsApiClient.getOdds(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(List.of(event1, event2));

            LiveOddsResult result = oddsService.getLiveOdds("nfl", "evt-1", null);

            assertThat(result.events()).hasSize(1);
            assertThat(result.events().getFirst().id()).isEqualTo("evt-1");
            assertThat(result.events().getFirst().homeTeam()).isEqualTo("Chiefs");
        }

        @Test
        void returnsEmptyListWhenEventIdMatchesNothing() {
            OddsResponse event = buildOddsResponse("evt-1", "Chiefs", "Ravens");
            when(oddsApiClient.getOdds(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(List.of(event));

            LiveOddsResult result = oddsService.getLiveOdds("nfl", "nonexistent", null);

            assertThat(result.events()).isEmpty();
            assertThat(result.eventCount()).isZero();
        }

        @Test
        void usesDefaultMarketWhenMarketIsNull() {
            when(oddsApiClient.getOdds(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(List.of());

            LiveOddsResult result = oddsService.getLiveOdds("nfl", null, null);

            assertThat(result.market()).isEqualTo("h2h");
        }

        @Test
        void usesDefaultMarketWhenMarketIsBlank() {
            when(oddsApiClient.getOdds(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(List.of());

            LiveOddsResult result = oddsService.getLiveOdds("nfl", null, "  ");

            assertThat(result.market()).isEqualTo("h2h");
        }

        @Test
        void usesProvidedMarketWhenNotBlank() {
            when(oddsApiClient.getOdds(anyString(), anyString(), eq("spreads"), anyString()))
                    .thenReturn(List.of());

            LiveOddsResult result = oddsService.getLiveOdds("nfl", null, "spreads");

            assertThat(result.market()).isEqualTo("spreads");
            verify(oddsApiClient).getOdds("americanfootball_nfl", "us", "spreads", "american");
        }

        @Test
        void mapsBookmakerDataCorrectly() {
            OddsResponse event = buildOddsResponse("evt-1", "Chiefs", "Ravens");
            when(oddsApiClient.getOdds(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(List.of(event));

            LiveOddsResult result = oddsService.getLiveOdds("nfl", null, null);

            var eventOdds = result.events().getFirst();
            assertThat(eventOdds.bookmakers()).hasSize(1);
            assertThat(eventOdds.bookmakers().getFirst().key()).isEqualTo("draftkings");
            assertThat(eventOdds.bookmakers().getFirst().markets()).hasSize(1);
            assertThat(eventOdds.bookmakers().getFirst().markets().getFirst().outcomes())
                    .hasSize(2);
        }

        @Test
        void returnsEmptyResultOnClientException() {
            when(oddsApiClient.getOdds(anyString(), anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("API unavailable"));

            LiveOddsResult result = oddsService.getLiveOdds("nfl", null, null);

            assertThat(result.events()).isEmpty();
            assertThat(result.eventCount()).isZero();
            assertThat(result.sport()).isEqualTo("nfl");
        }
    }

    @Nested
    class GetHistoricalOdds {

        @Test
        void delegatesToClientWithCorrectArguments() {
            OddsHistoryResponse historyResponse =
                    new OddsHistoryResponse("2026-01-15T20:00:00Z", null, null, List.of());
            when(oddsApiClient.getHistoricalOdds(
                            eq("americanfootball_nfl"), eq("us"), eq("h2h"), eq("2026-01-15T20:00:00Z")))
                    .thenReturn(historyResponse);

            oddsService.getHistoricalOdds("nfl", "2026-01-15T20:00:00Z");

            verify(oddsApiClient).getHistoricalOdds("americanfootball_nfl", "us", "h2h", "2026-01-15T20:00:00Z");
        }

        @Test
        void returnsEventsFromDataField() {
            OddsResponse event = buildOddsResponse("evt-1", "Chiefs", "Ravens");
            OddsHistoryResponse historyResponse =
                    new OddsHistoryResponse("2026-01-15T20:00:00Z", null, null, List.of(event));
            when(oddsApiClient.getHistoricalOdds(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(historyResponse);

            HistoricalOddsResult result = oddsService.getHistoricalOdds("nfl", "2026-01-15T20:00:00Z");

            assertThat(result.events()).hasSize(1);
            assertThat(result.events().getFirst().id()).isEqualTo("evt-1");
            assertThat(result.sport()).isEqualTo("nfl");
            assertThat(result.date()).isEqualTo("2026-01-15T20:00:00Z");
            assertThat(result.timestamp()).isEqualTo("2026-01-15T20:00:00Z");
        }

        @Test
        void returnsEmptyResultOnClientException() {
            when(oddsApiClient.getHistoricalOdds(anyString(), anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("API unavailable"));

            HistoricalOddsResult result = oddsService.getHistoricalOdds("nfl", "2026-01-15T20:00:00Z");

            assertThat(result.events()).isEmpty();
            assertThat(result.timestamp()).isNull();
            assertThat(result.sport()).isEqualTo("nfl");
        }

        @Test
        void multipleEventsAreMappedCorrectly() {
            OddsResponse event1 = buildOddsResponse("evt-1", "Chiefs", "Ravens");
            OddsResponse event2 = buildOddsResponse("evt-2", "Eagles", "49ers");
            OddsHistoryResponse historyResponse =
                    new OddsHistoryResponse("2026-01-10T00:00:00Z", null, null, List.of(event1, event2));
            when(oddsApiClient.getHistoricalOdds(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(historyResponse);

            HistoricalOddsResult result = oddsService.getHistoricalOdds("nfl", "2026-01-10T00:00:00Z");

            assertThat(result.events()).hasSize(2);
            assertThat(result.events()).extracting("id").containsExactly("evt-1", "evt-2");
        }
    }
}
