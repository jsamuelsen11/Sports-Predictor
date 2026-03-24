package com.sportspredictor.client.oddsapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/** WireMock tests for {@link OddsApiClient}. */
@WireMockTest
class OddsApiClientTest {

    private OddsApiClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        RestClient restClient =
                RestClient.builder().baseUrl(wmInfo.getHttpBaseUrl()).build();
        client = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(OddsApiClient.class);
    }

    @Nested
    class GetSports {

        @Test
        void returnsAvailableSports() {
            WireMock.stubFor(get(urlPathEqualTo("/sports"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("wiremock/odds-api/sports.json"))));

            List<SportResponse> sports = client.getSports();

            assertThat(sports).hasSize(2);
            assertThat(sports.getFirst().key()).isEqualTo("americanfootball_nfl");
            assertThat(sports.getFirst().title()).isEqualTo("NFL");
            assertThat(sports.getFirst().active()).isTrue();
        }
    }

    @Nested
    class GetOdds {

        @Test
        void returnsOddsWithBookmakers() {
            WireMock.stubFor(get(urlPathEqualTo("/sports/americanfootball_nfl/odds"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("wiremock/odds-api/odds-nfl-h2h.json"))));

            List<OddsResponse> odds = client.getOdds("americanfootball_nfl", "us", "h2h", "american");

            assertThat(odds).hasSize(1);

            OddsResponse event = odds.getFirst();
            assertThat(event.homeTeam()).isEqualTo("Buffalo Bills");
            assertThat(event.awayTeam()).isEqualTo("Kansas City Chiefs");
            assertThat(event.bookmakers()).hasSize(1);

            OddsResponse.Bookmaker bookmaker = event.bookmakers().getFirst();
            assertThat(bookmaker.key()).isEqualTo("fanduel");
            assertThat(bookmaker.markets()).hasSize(1);

            OddsResponse.Market market = bookmaker.markets().getFirst();
            assertThat(market.key()).isEqualTo("h2h");
            assertThat(market.outcomes()).hasSize(2);
        }

        @Test
        void parsesOddsOutcomePrices() {
            WireMock.stubFor(get(urlPathEqualTo("/sports/americanfootball_nfl/odds"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("wiremock/odds-api/odds-nfl-h2h.json"))));

            List<OddsResponse> odds = client.getOdds("americanfootball_nfl", "us", null, null);

            OddsResponse.Outcome favorite = odds.getFirst()
                    .bookmakers()
                    .getFirst()
                    .markets()
                    .getFirst()
                    .outcomes()
                    .getFirst();
            assertThat(favorite.name()).isEqualTo("Buffalo Bills");
            assertThat(favorite.price()).isEqualTo(-150.0);
        }
    }

    @Nested
    class GetScores {

        @Test
        void returnsCompletedScores() {
            WireMock.stubFor(get(urlPathEqualTo("/sports/americanfootball_nfl/scores"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("wiremock/odds-api/scores-nfl.json"))));

            List<ScoreResponse> scores = client.getScores("americanfootball_nfl", 3);

            assertThat(scores).hasSize(1);

            ScoreResponse score = scores.getFirst();
            assertThat(score.completed()).isTrue();
            assertThat(score.homeTeam()).isEqualTo("Buffalo Bills");
            assertThat(score.scores()).hasSize(2);
            assertThat(score.scores().getFirst().score()).isEqualTo("27");
        }
    }

    @Nested
    class GetHistoricalOdds {

        @Test
        void returnsHistoricalOddsWithTimestamps() {
            WireMock.stubFor(get(urlPathEqualTo("/sports/americanfootball_nfl/odds-history"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("wiremock/odds-api/odds-history.json"))));

            OddsHistoryResponse response =
                    client.getHistoricalOdds("americanfootball_nfl", "us", "h2h", "2026-01-10T12:00:00Z");

            assertThat(response.timestamp()).isEqualTo("2026-01-10T12:00:00Z");
            assertThat(response.previousTimestamp()).isEqualTo("2026-01-10T11:00:00Z");
            assertThat(response.nextTimestamp()).isEqualTo("2026-01-10T13:00:00Z");
            assertThat(response.data()).hasSize(1);
            assertThat(response.data().getFirst().sportKey()).isEqualTo("americanfootball_nfl");
        }
    }

    private static String loadFixture(String path) {
        try (var stream = OddsApiClientTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Fixture not found: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load fixture: " + path, e);
        }
    }
}
