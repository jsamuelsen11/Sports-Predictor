package com.sportspredictor.tool.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.service.InjuryService;
import com.sportspredictor.service.InjuryService.InjuryInfo;
import com.sportspredictor.service.InjuryService.InjuryReportResult;
import com.sportspredictor.service.InjuryService.TeamInjuryInfo;
import com.sportspredictor.service.ResultsService;
import com.sportspredictor.service.ResultsService.GameResult;
import com.sportspredictor.service.ResultsService.GameResultsResult;
import com.sportspredictor.service.ResultsService.TeamResult;
import com.sportspredictor.service.ScheduleService;
import com.sportspredictor.service.ScheduleService.GameInfo;
import com.sportspredictor.service.ScheduleService.ScheduleResult;
import com.sportspredictor.service.ScheduleService.TeamInfo;
import com.sportspredictor.service.WeatherService;
import com.sportspredictor.service.WeatherService.HourlyWeather;
import com.sportspredictor.service.WeatherService.WeatherResult;
import com.sportspredictor.tool.ingestion.GameIngestionTool.GameResultsResponse;
import com.sportspredictor.tool.ingestion.GameIngestionTool.InjuryReportResponse;
import com.sportspredictor.tool.ingestion.GameIngestionTool.ScheduleResponse;
import com.sportspredictor.tool.ingestion.GameIngestionTool.WeatherResponse;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link GameIngestionTool}. */
@ExtendWith(MockitoExtension.class)
class GameIngestionToolTest {

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private InjuryService injuryService;

    @Mock
    private WeatherService weatherService;

    @Mock
    private ResultsService resultsService;

    @InjectMocks
    private GameIngestionTool gameIngestionTool;

    // --- Fixture builders ---

    private static GameInfo buildGameInfo(String id) {
        TeamInfo home = new TeamInfo("1", "Los Angeles Lakers", "LAL", "110", false);
        TeamInfo away = new TeamInfo("2", "Boston Celtics", "BOS", "108", true);
        return new GameInfo(id, "2026-01-15T21:00:00Z", "Lakers vs Celtics", "LAL @ BOS", List.of(home, away), false);
    }

    private static TeamInjuryInfo buildTeamInjuryInfo(String teamName, int count) {
        List<InjuryInfo> injuries = java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new InjuryInfo("a" + i, "Player " + i, "WR", "Out", "Knee"))
                .toList();
        return new TeamInjuryInfo("t1", teamName, "ABR", injuries);
    }

    private static HourlyWeather buildHourlyWeather(String time) {
        return new HourlyWeather(time, 15.0, 0.0, 12.5, 65);
    }

    private static GameResult buildGameResult(String id) {
        TeamResult home = new TeamResult("t1", "Los Angeles Lakers", "LAL", "110", false);
        TeamResult away = new TeamResult("t2", "Boston Celtics", "BOS", "108", true);
        return new GameResult(id, "2026-01-15T21:00:00Z", "Lakers vs Celtics", List.of(home, away));
    }

    @Nested
    class GetGameSchedule {

        @Test
        void delegatesToScheduleService() {
            when(scheduleService.getGameSchedule("nba", "20260115", "Lakers"))
                    .thenReturn(new ScheduleResult("nba", "20260115", List.of(), 0));

            gameIngestionTool.getGameSchedule("nba", "20260115", "Lakers");

            verify(scheduleService).getGameSchedule("nba", "20260115", "Lakers");
        }

        @Test
        void buildsSummaryForEmptyResult() {
            when(scheduleService.getGameSchedule("nfl", null, null))
                    .thenReturn(new ScheduleResult("nfl", null, List.of(), 0));

            ScheduleResponse response = gameIngestionTool.getGameSchedule("nfl", null, null);

            assertThat(response.summary()).contains("No games found");
            assertThat(response.summary()).contains("nfl");
        }

        @Test
        void buildsSummaryWithGameCountForNonEmptyResult() {
            GameInfo game = buildGameInfo("g1");
            when(scheduleService.getGameSchedule("nba", "20260115", null))
                    .thenReturn(new ScheduleResult("nba", "20260115", List.of(game), 1));

            ScheduleResponse response = gameIngestionTool.getGameSchedule("nba", "20260115", null);

            assertThat(response.summary()).contains("1");
            assertThat(response.summary()).contains("NBA");
            assertThat(response.summary()).contains("20260115");
        }

        @Test
        void summaryIncludesTeamWhenProvided() {
            GameInfo game = buildGameInfo("g1");
            when(scheduleService.getGameSchedule("nba", null, "Lakers"))
                    .thenReturn(new ScheduleResult("nba", null, List.of(game), 1));

            ScheduleResponse response = gameIngestionTool.getGameSchedule("nba", null, "Lakers");

            assertThat(response.summary()).contains("Lakers");
        }

        @Test
        void passesAllServiceResultFieldsThrough() {
            GameInfo game = buildGameInfo("g1");
            when(scheduleService.getGameSchedule("nba", "20260115", null))
                    .thenReturn(new ScheduleResult("nba", "20260115", List.of(game), 1));

            ScheduleResponse response = gameIngestionTool.getGameSchedule("nba", "20260115", null);

            assertThat(response.sport()).isEqualTo("nba");
            assertThat(response.dateRange()).isEqualTo("20260115");
            assertThat(response.games()).hasSize(1);
            assertThat(response.gameCount()).isEqualTo(1);
        }
    }

    @Nested
    class GetInjuryReport {

        @Test
        void delegatesToInjuryService() {
            when(injuryService.getInjuryReport("nfl", "Chiefs"))
                    .thenReturn(new InjuryReportResult("nfl", "Chiefs", List.of(), 0));

            gameIngestionTool.getInjuryReport("nfl", "Chiefs");

            verify(injuryService).getInjuryReport("nfl", "Chiefs");
        }

        @Test
        void buildsSummaryForZeroInjuries() {
            when(injuryService.getInjuryReport("nfl", null))
                    .thenReturn(new InjuryReportResult("nfl", null, List.of(), 0));

            InjuryReportResponse response = gameIngestionTool.getInjuryReport("nfl", null);

            assertThat(response.summary()).contains("No injuries reported");
            assertThat(response.summary()).contains("nfl");
        }

        @Test
        void buildsSummaryWithInjuryAndTeamCountsForNonEmptyResult() {
            TeamInjuryInfo chiefs = buildTeamInjuryInfo("Kansas City Chiefs", 3);
            TeamInjuryInfo eagles = buildTeamInjuryInfo("Philadelphia Eagles", 2);
            when(injuryService.getInjuryReport("nfl", null))
                    .thenReturn(new InjuryReportResult("nfl", null, List.of(chiefs, eagles), 5));

            InjuryReportResponse response = gameIngestionTool.getInjuryReport("nfl", null);

            assertThat(response.summary()).contains("5"); // total injuries
            assertThat(response.summary()).contains("2"); // team count
            assertThat(response.summary()).contains("NFL");
        }

        @Test
        void summaryIncludesTeamFilterWhenProvided() {
            TeamInjuryInfo chiefs = buildTeamInjuryInfo("Kansas City Chiefs", 2);
            when(injuryService.getInjuryReport("nfl", "Chiefs"))
                    .thenReturn(new InjuryReportResult("nfl", "Chiefs", List.of(chiefs), 2));

            InjuryReportResponse response = gameIngestionTool.getInjuryReport("nfl", "Chiefs");

            assertThat(response.summary()).contains("Chiefs");
        }

        @Test
        void passesAllServiceResultFieldsThrough() {
            TeamInjuryInfo chiefs = buildTeamInjuryInfo("Kansas City Chiefs", 2);
            when(injuryService.getInjuryReport("nfl", null))
                    .thenReturn(new InjuryReportResult("nfl", null, List.of(chiefs), 2));

            InjuryReportResponse response = gameIngestionTool.getInjuryReport("nfl", null);

            assertThat(response.sport()).isEqualTo("nfl");
            assertThat(response.team()).isNull();
            assertThat(response.teamInjuries()).hasSize(1);
            assertThat(response.totalInjuries()).isEqualTo(2);
        }
    }

    @Nested
    class GetWeatherForecast {

        private static final double LAT = 39.0997;
        private static final double LON = -94.5786;

        @Test
        void delegatesToWeatherService() {
            when(weatherService.getWeatherForecast(LAT, LON, "2026-01-20"))
                    .thenReturn(new WeatherResult(LAT, LON, "America/Chicago", List.of()));

            gameIngestionTool.getWeatherForecast(LAT, LON, "2026-01-20");

            verify(weatherService).getWeatherForecast(LAT, LON, "2026-01-20");
        }

        @Test
        void buildsSummaryForEmptyForecasts() {
            when(weatherService.getWeatherForecast(LAT, LON, "2026-01-20"))
                    .thenReturn(new WeatherResult(LAT, LON, null, List.of()));

            WeatherResponse response = gameIngestionTool.getWeatherForecast(LAT, LON, "2026-01-20");

            assertThat(response.summary()).contains("No weather data available");
            assertThat(response.summary()).contains("2026-01-20");
        }

        @Test
        void buildsSummaryWithHourlyCountForNonEmptyResult() {
            List<HourlyWeather> forecasts = List.of(
                    buildHourlyWeather("2026-01-20T18:00"),
                    buildHourlyWeather("2026-01-20T19:00"),
                    buildHourlyWeather("2026-01-20T20:00"));
            when(weatherService.getWeatherForecast(LAT, LON, "2026-01-20"))
                    .thenReturn(new WeatherResult(LAT, LON, "America/Chicago", forecasts));

            WeatherResponse response = gameIngestionTool.getWeatherForecast(LAT, LON, "2026-01-20");

            assertThat(response.summary()).contains("3");
            assertThat(response.summary()).contains("America/Chicago");
        }

        @Test
        void passesAllServiceResultFieldsThrough() {
            List<HourlyWeather> forecasts = List.of(buildHourlyWeather("2026-01-20T18:00"));
            when(weatherService.getWeatherForecast(LAT, LON, "2026-01-20"))
                    .thenReturn(new WeatherResult(LAT, LON, "America/Chicago", forecasts));

            WeatherResponse response = gameIngestionTool.getWeatherForecast(LAT, LON, "2026-01-20");

            assertThat(response.latitude()).isEqualTo(LAT);
            assertThat(response.longitude()).isEqualTo(LON);
            assertThat(response.gameDate()).isEqualTo("2026-01-20");
            assertThat(response.hourlyForecasts()).hasSize(1);
        }
    }

    @Nested
    class GetGameResults {

        @Test
        void delegatesToResultsService() {
            when(resultsService.getGameResults("nba", "20260115", null))
                    .thenReturn(new GameResultsResult("nba", "20260115", List.of(), 0));

            gameIngestionTool.getGameResults("nba", "20260115", null);

            verify(resultsService).getGameResults("nba", "20260115", null);
        }

        @Test
        void buildsSummaryForEmptyResult() {
            when(resultsService.getGameResults("nfl", null, null))
                    .thenReturn(new GameResultsResult("nfl", null, List.of(), 0));

            GameResultsResponse response = gameIngestionTool.getGameResults("nfl", null, null);

            assertThat(response.summary()).contains("No completed games found");
            assertThat(response.summary()).contains("nfl");
        }

        @Test
        void buildsSummaryWithGameCountForNonEmptyResult() {
            GameResult result = buildGameResult("g1");
            when(resultsService.getGameResults("nba", "20260115", null))
                    .thenReturn(new GameResultsResult("nba", "20260115", List.of(result), 1));

            GameResultsResponse response = gameIngestionTool.getGameResults("nba", "20260115", null);

            assertThat(response.summary()).contains("1");
            assertThat(response.summary()).contains("NBA");
            assertThat(response.summary()).contains("20260115");
        }

        @Test
        void passesAllServiceResultFieldsThrough() {
            GameResult result = buildGameResult("g1");
            when(resultsService.getGameResults("nba", "20260115", "g1"))
                    .thenReturn(new GameResultsResult("nba", "20260115", List.of(result), 1));

            GameResultsResponse response = gameIngestionTool.getGameResults("nba", "20260115", "g1");

            assertThat(response.sport()).isEqualTo("nba");
            assertThat(response.date()).isEqualTo("20260115");
            assertThat(response.results()).hasSize(1);
            assertThat(response.gameCount()).isEqualTo(1);
        }
    }
}
