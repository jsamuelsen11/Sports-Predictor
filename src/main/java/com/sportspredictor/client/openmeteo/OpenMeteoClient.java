package com.sportspredictor.client.openmeteo;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Open-Meteo weather API client — no authentication required. */
@HttpExchange
public interface OpenMeteoClient {

    /** Fetches hourly weather forecast for the given coordinates. */
    @GetExchange("/forecast")
    ForecastResponse getForecast(
            @RequestParam("latitude") double latitude,
            @RequestParam("longitude") double longitude,
            @RequestParam("hourly") String hourly,
            @RequestParam(value = "forecast_days", required = false) Integer forecastDays);
}
