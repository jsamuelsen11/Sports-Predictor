package com.sportspredictor.client.openmeteo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Open-Meteo forecast API response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ForecastResponse(
        double latitude,
        double longitude,
        @JsonProperty("elevation") double elevation,
        @JsonProperty("timezone") String timezone,
        @JsonProperty("hourly_units") HourlyUnits hourlyUnits,
        @JsonProperty("hourly") Hourly hourly) {

    /** Units for each hourly parameter. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HourlyUnits(
            String time,
            @JsonProperty("temperature_2m") String temperature2m,
            @JsonProperty("precipitation") String precipitation,
            @JsonProperty("wind_speed_10m") String windSpeed10m,
            @JsonProperty("relative_humidity_2m") String relativeHumidity2m) {}

    /** Hourly forecast data arrays — each list is aligned by index. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Hourly(
            List<String> time,
            @JsonProperty("temperature_2m") List<Double> temperature2m,
            @JsonProperty("precipitation") List<Double> precipitation,
            @JsonProperty("wind_speed_10m") List<Double> windSpeed10m,
            @JsonProperty("relative_humidity_2m") List<Integer> relativeHumidity2m) {}
}
