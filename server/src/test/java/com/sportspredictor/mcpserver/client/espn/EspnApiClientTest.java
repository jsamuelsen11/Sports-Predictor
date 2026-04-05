package com.sportspredictor.mcpserver.client.espn;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.sportspredictor.mcpserver.client.WireMockFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/** WireMock tests for {@link EspnApiClient}. */
@WireMockTest
class EspnApiClientTest {

    private EspnApiClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        RestClient restClient =
                RestClient.builder().baseUrl(wmInfo.getHttpBaseUrl()).build();
        client = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(EspnApiClient.class);
    }

    @Nested
    class GetScoreboard {

        @Test
        void returnsNflScoreboard() {
            WireMock.stubFor(get(urlPathEqualTo("/apis/site/v2/sports/football/nfl/scoreboard"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(WireMockFixtures.load("wiremock/espn/scoreboard-nfl.json"))));

            ScoreboardResponse response = client.getScoreboard("football", "nfl");

            assertThat(response).isNotNull();
            assertThat(response.events()).hasSize(1);

            ScoreboardResponse.Event event = response.events().getFirst();
            assertThat(event.name()).isEqualTo("Kansas City Chiefs at Buffalo Bills");
            assertThat(event.shortName()).isEqualTo("KC @ BUF");
        }

        @Test
        void parsesCompetitorsAndScores() {
            WireMock.stubFor(get(urlPathEqualTo("/apis/site/v2/sports/football/nfl/scoreboard"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(WireMockFixtures.load("wiremock/espn/scoreboard-nfl.json"))));

            ScoreboardResponse response = client.getScoreboard("football", "nfl");

            ScoreboardResponse.Competition competition =
                    response.events().getFirst().competitions().getFirst();
            assertThat(competition.competitors()).hasSize(2);

            ScoreboardResponse.Competitor winner = competition.competitors().stream()
                    .filter(ScoreboardResponse.Competitor::winner)
                    .findFirst()
                    .orElseThrow();
            assertThat(winner.team().displayName()).isEqualTo("Buffalo Bills");
            assertThat(winner.score()).isEqualTo("27");
        }

        @Test
        void parsesGameStatus() {
            WireMock.stubFor(get(urlPathEqualTo("/apis/site/v2/sports/football/nfl/scoreboard"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(WireMockFixtures.load("wiremock/espn/scoreboard-nfl.json"))));

            ScoreboardResponse response = client.getScoreboard("football", "nfl");

            ScoreboardResponse.Status status =
                    response.events().getFirst().competitions().getFirst().status();
            assertThat(status.type().name()).isEqualTo("STATUS_FINAL");
            assertThat(status.type().completed()).isTrue();
        }
    }

    @Nested
    class GetStandings {

        @Test
        void returnsNbaStandings() {
            WireMock.stubFor(get(urlPathEqualTo("/apis/site/v2/sports/basketball/nba/standings"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(WireMockFixtures.load("wiremock/espn/standings-nba.json"))));

            StandingsResponse response = client.getStandings("basketball", "nba");

            assertThat(response).isNotNull();
            assertThat(response.children()).hasSize(1);
            assertThat(response.children().getFirst().name()).isEqualTo("Eastern Conference");
        }
    }

    @Nested
    class GetInjuries {

        @Test
        void returnsNflInjuries() {
            WireMock.stubFor(get(urlPathEqualTo("/apis/site/v2/sports/football/nfl/injuries"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(WireMockFixtures.load("wiremock/espn/injuries-nfl.json"))));

            InjuryResponse response = client.getInjuries("football", "nfl");

            assertThat(response).isNotNull();
            assertThat(response.injuries()).hasSize(1);

            InjuryResponse.TeamInjuries teamInjuries = response.injuries().getFirst();
            assertThat(teamInjuries.team().abbreviation()).isEqualTo("KC");
            assertThat(teamInjuries.injuries()).hasSize(1);

            InjuryResponse.Injury injury = teamInjuries.injuries().getFirst();
            assertThat(injury.athlete().displayName()).isEqualTo("Patrick Mahomes");
            assertThat(injury.status()).isEqualTo("Questionable");
        }
    }
}
