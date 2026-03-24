package com.sportspredictor.client.apisports;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/** WireMock tests for {@link ApiSportsClient}. */
@WireMockTest
class ApiSportsClientTest {

    private ApiSportsClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        RestClient restClient =
                RestClient.builder().baseUrl(wmInfo.getHttpBaseUrl()).build();
        client = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(ApiSportsClient.class);
    }

    @Nested
    class GetFixtures {

        @Test
        void returnsFixturesForLeague() {
            WireMock.stubFor(get(urlPathEqualTo("/fixtures"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("wiremock/api-sports/fixtures.json"))));

            ApiSportsResponse<FixtureData> response = client.getFixtures(39, 2025, "2026-03-22");

            assertThat(response).isNotNull();
            assertThat(response.results()).isEqualTo(1);
            assertThat(response.response()).hasSize(1);

            FixtureData fixture = response.response().getFirst();
            assertThat(fixture.fixture().id()).isEqualTo(1035024);
            assertThat(fixture.league().name()).isEqualTo("Premier League");
        }

        @Test
        void parsesTeamsAndScore() {
            WireMock.stubFor(get(urlPathEqualTo("/fixtures"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("wiremock/api-sports/fixtures.json"))));

            ApiSportsResponse<FixtureData> response = client.getFixtures(39, 2025, null);

            FixtureData fixture = response.response().getFirst();
            assertThat(fixture.teams().home().name()).isEqualTo("Manchester United");
            assertThat(fixture.teams().home().winner()).isTrue();
            assertThat(fixture.goals().home()).isEqualTo(2);
            assertThat(fixture.goals().away()).isEqualTo(1);
        }

        @Test
        void parsesScoreBreakdown() {
            WireMock.stubFor(get(urlPathEqualTo("/fixtures"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("wiremock/api-sports/fixtures.json"))));

            ApiSportsResponse<FixtureData> response = client.getFixtures(39, 2025, null);

            FixtureData.Score score = response.response().getFirst().score();
            assertThat(score.halftime().home()).isEqualTo(1);
            assertThat(score.halftime().away()).isEqualTo(0);
            assertThat(score.fulltime().home()).isEqualTo(2);
            assertThat(score.fulltime().away()).isEqualTo(1);
        }
    }

    @Nested
    class GetTeams {

        @Test
        void returnsTeamsForLeague() {
            WireMock.stubFor(get(urlPathEqualTo("/teams"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("wiremock/api-sports/teams.json"))));

            ApiSportsResponse<TeamData> response = client.getTeams(39, 2025);

            assertThat(response.results()).isEqualTo(1);

            TeamData teamData = response.response().getFirst();
            assertThat(teamData.team().name()).isEqualTo("Manchester United");
            assertThat(teamData.team().founded()).isEqualTo(1878);
            assertThat(teamData.venue().name()).isEqualTo("Old Trafford");
            assertThat(teamData.venue().capacity()).isEqualTo(76212);
        }
    }

    @Nested
    class GetPaging {

        @Test
        void parsesPagingMetadata() {
            WireMock.stubFor(get(urlPathEqualTo("/fixtures"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("wiremock/api-sports/fixtures.json"))));

            ApiSportsResponse<FixtureData> response = client.getFixtures(39, 2025, null);

            assertThat(response.paging().current()).isEqualTo(1);
            assertThat(response.paging().total()).isEqualTo(1);
        }
    }

    private static String loadFixture(String path) {
        try (var stream = ApiSportsClientTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Fixture not found: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load fixture: " + path, e);
        }
    }
}
