package com.sportspredictor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.client.openmeteo.ForecastResponse;
import com.sportspredictor.client.openmeteo.OpenMeteoClient;
import com.sportspredictor.service.WeatherService.WeatherResult;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link WeatherService}. */
@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private OpenMeteoClient openMeteoClient;

    @InjectMocks
    private WeatherService weatherService;

    private static final double LAT = 39.0997;
    private static final double LON = -94.5786;
    private static final String HOURLY_PARAMS = "temperature_2m,precipitation,wind_speed_10m,relative_humidity_2m";

    private static ForecastResponse buildForecastResponse(int hourCount) {
        List<String> times = java.util.stream.IntStream.range(0, hourCount)
                .mapToObj(i -> "2026-01-15T" + String.format("%02d", i % 24) + ":00")
                .toList();
        List<Double> temps = java.util.Collections.nCopies(hourCount, 15.0);
        List<Double> precip = java.util.Collections.nCopies(hourCount, 0.0);
        List<Double> wind = java.util.Collections.nCopies(hourCount, 12.5);
        List<Integer> humidity = java.util.Collections.nCopies(hourCount, 65);
        ForecastResponse.Hourly hourly = new ForecastResponse.Hourly(times, temps, precip, wind, humidity);
        ForecastResponse.HourlyUnits units = new ForecastResponse.HourlyUnits("iso8601", "°C", "mm", "km/h", "%");
        return new ForecastResponse(LAT, LON, 265.0, "America/Chicago", units, hourly);
    }

    @Nested
    class GetWeatherForecast {

        @Test
        void delegatesToClientWithCorrectCoordinates() {
            when(openMeteoClient.getForecast(eq(LAT), eq(LON), anyString(), anyInt()))
                    .thenReturn(buildForecastResponse(24));

            weatherService.getWeatherForecast(LAT, LON, null);

            verify(openMeteoClient).getForecast(eq(LAT), eq(LON), eq(HOURLY_PARAMS), anyInt());
        }

        @Test
        void returnsHourlyForecastsFromResponse() {
            when(openMeteoClient.getForecast(eq(LAT), eq(LON), anyString(), anyInt()))
                    .thenReturn(buildForecastResponse(24));

            WeatherResult result = weatherService.getWeatherForecast(LAT, LON, null);

            assertThat(result.latitude()).isEqualTo(LAT);
            assertThat(result.longitude()).isEqualTo(LON);
            assertThat(result.timezone()).isEqualTo("America/Chicago");
            assertThat(result.hourlyForecasts()).hasSize(24);
        }

        @Test
        void mapsHourlyFieldsCorrectly() {
            when(openMeteoClient.getForecast(eq(LAT), eq(LON), anyString(), anyInt()))
                    .thenReturn(buildForecastResponse(1));

            WeatherResult result = weatherService.getWeatherForecast(LAT, LON, null);

            var hour = result.hourlyForecasts().getFirst();
            assertThat(hour.temperatureCelsius()).isEqualTo(15.0);
            assertThat(hour.precipitationMm()).isEqualTo(0.0);
            assertThat(hour.windSpeedKmh()).isEqualTo(12.5);
            assertThat(hour.relativeHumidity()).isEqualTo(65);
        }

        @Test
        void returnsEmptyForecastsWhenHourlyIsNull() {
            ForecastResponse.HourlyUnits units = new ForecastResponse.HourlyUnits("iso8601", "°C", "mm", "km/h", "%");
            ForecastResponse emptyResponse = new ForecastResponse(LAT, LON, 265.0, "UTC", units, null);
            when(openMeteoClient.getForecast(eq(LAT), eq(LON), anyString(), anyInt()))
                    .thenReturn(emptyResponse);

            WeatherResult result = weatherService.getWeatherForecast(LAT, LON, null);

            assertThat(result.hourlyForecasts()).isEmpty();
        }

        @Test
        void returnsErrorResultOnClientException() {
            when(openMeteoClient.getForecast(eq(LAT), eq(LON), anyString(), anyInt()))
                    .thenThrow(new RuntimeException("Open-Meteo unavailable"));

            WeatherResult result = weatherService.getWeatherForecast(LAT, LON, "2026-01-20");

            assertThat(result.hourlyForecasts()).isEmpty();
            assertThat(result.timezone()).isNull();
            assertThat(result.latitude()).isEqualTo(LAT);
            assertThat(result.longitude()).isEqualTo(LON);
        }
    }

    @Nested
    class CalculateForecastDays {

        @Test
        void returnsOneForNullDate() {
            assertThat(weatherService.calculateForecastDays(null)).isEqualTo(1);
        }

        @Test
        void returnsOneForBlankDate() {
            assertThat(weatherService.calculateForecastDays("  ")).isEqualTo(1);
        }

        @Test
        void returnsOneForUnparseableDate() {
            assertThat(weatherService.calculateForecastDays("not-a-date")).isEqualTo(1);
        }

        @Test
        void returnsOneForTodaysDate() {
            String today = LocalDate.now(ZoneId.systemDefault()).toString();
            // today -> days difference is 0, clamped to minimum of 1
            assertThat(weatherService.calculateForecastDays(today)).isEqualTo(1);
        }

        @Test
        void returnsCorrectDaysForNearFutureDate() {
            String threeDaysFromNow =
                    LocalDate.now(ZoneId.systemDefault()).plusDays(3).toString();
            // days difference = 3, +1 = 4
            assertThat(weatherService.calculateForecastDays(threeDaysFromNow)).isEqualTo(4);
        }

        @Test
        void clampsToDaysMaximumOf16() {
            String farFuture =
                    LocalDate.now(ZoneId.systemDefault()).plusDays(100).toString();
            assertThat(weatherService.calculateForecastDays(farFuture)).isEqualTo(16);
        }

        @Test
        void returnsOneForPastDate() {
            String yesterday =
                    LocalDate.now(ZoneId.systemDefault()).minusDays(1).toString();
            // days difference = -1, +1 = 0, clamped to 1
            assertThat(weatherService.calculateForecastDays(yesterday)).isEqualTo(1);
        }

        @Test
        void returnsExactDaysForBoundaryDate() {
            // 15 days from now -> 15 + 1 = 16, which equals MAX_FORECAST_DAYS
            String boundary = LocalDate.now(ZoneId.systemDefault()).plusDays(15).toString();
            assertThat(weatherService.calculateForecastDays(boundary)).isEqualTo(16);
        }
    }
}
