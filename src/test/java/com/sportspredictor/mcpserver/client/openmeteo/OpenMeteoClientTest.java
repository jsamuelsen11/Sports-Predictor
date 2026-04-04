package com.sportspredictor.mcpserver.client.openmeteo;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.sportspredictor.mcpserver.client.WireMockFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/** WireMock tests for {@link OpenMeteoClient}. */
@WireMockTest
class OpenMeteoClientTest {

    private OpenMeteoClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        RestClient restClient =
                RestClient.builder().baseUrl(wmInfo.getHttpBaseUrl()).build();
        client = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(OpenMeteoClient.class);
    }

    @Nested
    class GetForecast {

        @Test
        void returnsForecastForValidCoordinates() {
            stubForecast();

            ForecastResponse response = client.getForecast(
                    40.7128, -74.0060, "temperature_2m,precipitation,wind_speed_10m,relative_humidity_2m", 1);

            assertThat(response).isNotNull();
            assertThat(response.latitude()).isCloseTo(40.71, org.assertj.core.data.Offset.offset(0.01));
            assertThat(response.timezone()).isEqualTo("GMT");
        }

        @Test
        void parsesHourlyData() {
            stubForecast();

            ForecastResponse response = client.getForecast(40.7128, -74.0060, "temperature_2m", 1);

            assertThat(response.hourly()).isNotNull();
            assertThat(response.hourly().time()).hasSize(3);
            assertThat(response.hourly().temperature2m()).containsExactly(8.5, 7.2, 6.8);
            assertThat(response.hourly().precipitation()).containsExactly(0.0, 0.1, 0.0);
            assertThat(response.hourly().windSpeed10m()).containsExactly(12.3, 14.1, 11.5);
            assertThat(response.hourly().relativeHumidity2m()).containsExactly(65, 70, 72);
        }

        @Test
        void parsesHourlyUnits() {
            stubForecast();

            ForecastResponse response = client.getForecast(40.7128, -74.0060, "temperature_2m", 1);

            assertThat(response.hourlyUnits()).isNotNull();
            assertThat(response.hourlyUnits().temperature2m()).isEqualTo("°C");
            assertThat(response.hourlyUnits().precipitation()).isEqualTo("mm");
        }

        private void stubForecast() {
            com.github.tomakehurst.wiremock.client.WireMock.stubFor(get(urlPathEqualTo("/forecast"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(WireMockFixtures.load("wiremock/open-meteo/forecast-response.json"))));
        }
    }
}
