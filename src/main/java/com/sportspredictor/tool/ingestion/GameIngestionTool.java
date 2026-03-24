package com.sportspredictor.tool.ingestion;

import com.sportspredictor.service.InjuryService;
import com.sportspredictor.service.ResultsService;
import com.sportspredictor.service.ScheduleService;
import com.sportspredictor.service.WeatherService;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tools for game schedules, injuries, weather, and results. */
@Service
@RequiredArgsConstructor
public class GameIngestionTool {

    private final ScheduleService scheduleService;
    private final InjuryService injuryService;
    private final WeatherService weatherService;
    private final ResultsService resultsService;

    /** Response for schedule queries. */
    public record ScheduleResponse(
            String sport, String dateRange, List<ScheduleService.GameInfo> games, int gameCount, String summary) {}

    /** Response for injury report queries. */
    public record InjuryReportResponse(
            String sport,
            String team,
            List<InjuryService.TeamInjuryInfo> teamInjuries,
            int totalInjuries,
            String summary) {}

    /** Response for weather forecast queries. */
    public record WeatherResponse(
            double latitude,
            double longitude,
            String gameDate,
            List<WeatherService.HourlyWeather> hourlyForecasts,
            String summary) {}

    /** Response for game results queries. */
    public record GameResultsResponse(
            String sport, String date, List<ResultsService.GameResult> results, int gameCount, String summary) {}

    /** Fetches the upcoming game schedule for a sport with optional date range and team filtering. */
    @Tool(
            name = "get_game_schedule",
            description = "Get upcoming game schedule for a sport. Returns games with teams, dates, and status."
                    + " Can filter by date range (YYYYMMDD or YYYYMMDD-YYYYMMDD) and team name.")
    public ScheduleResponse getGameSchedule(
            @ToolParam(description = "Sport key (e.g., nfl, nba, mlb, nhl, epl)") String sport,
            @ToolParam(description = "Date range in YYYYMMDD or YYYYMMDD-YYYYMMDD format", required = false)
                    String dateRange,
            @ToolParam(description = "Team name or abbreviation to filter", required = false) String team) {
        var result = scheduleService.getGameSchedule(sport, dateRange, team);

        String summary;
        if (result.gameCount() == 0) {
            summary = String.format(Locale.ROOT, "No games found for %s.", sport);
        } else {
            summary = String.format(
                    Locale.ROOT,
                    "Found %d %s games%s%s.",
                    result.gameCount(),
                    sport.toUpperCase(Locale.ROOT),
                    dateRange != null ? " for " + dateRange : "",
                    team != null ? " involving " + team : "");
        }

        return new ScheduleResponse(result.sport(), result.dateRange(), result.games(), result.gameCount(), summary);
    }

    /** Fetches the current injury report for a sport with optional team filtering. */
    @Tool(
            name = "get_injury_report",
            description = "Get current injury report for a sport showing player injuries with status"
                    + " (out/doubtful/questionable/probable). Can filter by team.")
    public InjuryReportResponse getInjuryReport(
            @ToolParam(description = "Sport key (e.g., nfl, nba, mlb, nhl, epl)") String sport,
            @ToolParam(description = "Team name or abbreviation to filter", required = false) String team) {
        var result = injuryService.getInjuryReport(sport, team);

        String summary;
        if (result.totalInjuries() == 0) {
            summary = String.format(
                    Locale.ROOT, "No injuries reported for %s%s.", sport, team != null ? " (" + team + ")" : "");
        } else {
            summary = String.format(
                    Locale.ROOT,
                    "%d injuries across %d teams for %s%s.",
                    result.totalInjuries(),
                    result.teamInjuries().size(),
                    sport.toUpperCase(Locale.ROOT),
                    team != null ? " (" + team + ")" : "");
        }

        return new InjuryReportResponse(
                result.sport(), result.team(), result.teamInjuries(), result.totalInjuries(), summary);
    }

    /** Fetches the weather forecast for a game venue at the given coordinates and date. */
    @Tool(
            name = "get_weather_forecast",
            description = "Get weather forecast for an outdoor game venue at game time. Provides temperature,"
                    + " precipitation, wind speed, and humidity. Only useful for outdoor sports.")
    public WeatherResponse getWeatherForecast(
            @ToolParam(description = "Venue latitude (e.g., 39.0997)") double latitude,
            @ToolParam(description = "Venue longitude (e.g., -94.5786)") double longitude,
            @ToolParam(description = "Game date in ISO-8601 format (e.g., 2026-03-25)") String gameDate) {
        var result = weatherService.getWeatherForecast(latitude, longitude, gameDate);

        String summary;
        if (result.hourlyForecasts().isEmpty()) {
            summary = String.format(
                    Locale.ROOT,
                    "No weather data available for lat=%.4f, lon=%.4f on %s.",
                    latitude,
                    longitude,
                    gameDate);
        } else {
            summary = String.format(
                    Locale.ROOT,
                    "Weather forecast for lat=%.4f, lon=%.4f: %d hourly readings available. Timezone: %s.",
                    result.latitude(),
                    result.longitude(),
                    result.hourlyForecasts().size(),
                    result.timezone());
        }

        return new WeatherResponse(result.latitude(), result.longitude(), gameDate, result.hourlyForecasts(), summary);
    }

    /** Fetches final scores and results for completed games with optional date and event ID filtering. */
    @Tool(
            name = "get_game_results",
            description = "Get final scores and results for completed games. Can filter by date and specific event ID.")
    public GameResultsResponse getGameResults(
            @ToolParam(description = "Sport key (e.g., nfl, nba, mlb, nhl, epl)") String sport,
            @ToolParam(description = "Date in YYYYMMDD format", required = false) String date,
            @ToolParam(description = "Specific event ID", required = false) String eventId) {
        var result = resultsService.getGameResults(sport, date, eventId);

        String summary;
        if (result.gameCount() == 0) {
            summary = String.format(
                    Locale.ROOT, "No completed games found for %s%s.", sport, date != null ? " on " + date : "");
        } else {
            summary = String.format(
                    Locale.ROOT,
                    "%d completed %s game(s)%s.",
                    result.gameCount(),
                    sport.toUpperCase(Locale.ROOT),
                    date != null ? " on " + date : "");
        }

        return new GameResultsResponse(result.sport(), result.date(), result.results(), result.gameCount(), summary);
    }
}
