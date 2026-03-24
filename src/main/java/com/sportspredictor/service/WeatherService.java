package com.sportspredictor.service;

import com.sportspredictor.client.openmeteo.ForecastResponse;
import com.sportspredictor.client.openmeteo.OpenMeteoClient;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Fetches weather forecasts from Open-Meteo with caching. */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private static final String HOURLY_PARAMS = "temperature_2m,precipitation,wind_speed_10m,relative_humidity_2m";
    private static final int MAX_FORECAST_DAYS = 16;
    private static final int MIN_FORECAST_DAYS = 1;

    private final OpenMeteoClient openMeteoClient;

    /** Weather forecast result. */
    public record WeatherResult(
            double latitude, double longitude, String timezone, List<HourlyWeather> hourlyForecasts) {}

    /** Weather data for a single hour. */
    public record HourlyWeather(
            String time,
            Double temperatureCelsius,
            Double precipitationMm,
            Double windSpeedKmh,
            Integer relativeHumidity) {}

    /** Returns a cached hourly weather forecast for the given coordinates and game date. */
    @Cacheable(value = "weather", key = "#latitude + '-' + #longitude + '-' + #gameDate")
    public WeatherResult getWeatherForecast(double latitude, double longitude, String gameDate) {
        int forecastDays = calculateForecastDays(gameDate);

        try {
            ForecastResponse response = openMeteoClient.getForecast(latitude, longitude, HOURLY_PARAMS, forecastDays);

            List<HourlyWeather> hourly = buildHourlyForecasts(response);

            return new WeatherResult(response.latitude(), response.longitude(), response.timezone(), hourly);
        } catch (Exception e) {
            log.warn(
                    "Failed to fetch weather for lat={}, lon={}, date={}: {}",
                    latitude,
                    longitude,
                    gameDate,
                    e.getMessage());
            return new WeatherResult(latitude, longitude, null, List.of());
        }
    }

    int calculateForecastDays(String gameDate) {
        if (gameDate == null || gameDate.isBlank()) {
            return MIN_FORECAST_DAYS;
        }
        try {
            LocalDate target = LocalDate.parse(gameDate);
            long days = ChronoUnit.DAYS.between(LocalDate.now(ZoneId.systemDefault()), target) + 1;
            return (int) Math.max(MIN_FORECAST_DAYS, Math.min(days, MAX_FORECAST_DAYS));
        } catch (Exception e) {
            log.warn("Could not parse game date '{}', using default forecast days", gameDate);
            return MIN_FORECAST_DAYS;
        }
    }

    private List<HourlyWeather> buildHourlyForecasts(ForecastResponse response) {
        if (response.hourly() == null || response.hourly().time() == null) {
            return List.of();
        }
        var hourly = response.hourly();
        int size = hourly.time().size();
        return java.util.stream.IntStream.range(0, size)
                .mapToObj(i -> new HourlyWeather(
                        hourly.time().get(i),
                        safeGet(hourly.temperature2m(), i),
                        safeGet(hourly.precipitation(), i),
                        safeGet(hourly.windSpeed10m(), i),
                        safeGetInt(hourly.relativeHumidity2m(), i)))
                .toList();
    }

    private static Double safeGet(List<Double> list, int index) {
        return (list != null && index < list.size()) ? list.get(index) : null;
    }

    private static Integer safeGetInt(List<Integer> list, int index) {
        return (list != null && index < list.size()) ? list.get(index) : null;
    }
}
