package com.sportspredictor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.repository.PredictionLogRepository;
import com.sportspredictor.service.InjuryService.InjuryReportResult;
import com.sportspredictor.service.OddsService.BookmakerOdds;
import com.sportspredictor.service.OddsService.EventOdds;
import com.sportspredictor.service.OddsService.LiveOddsResult;
import com.sportspredictor.service.OddsService.MarketOdds;
import com.sportspredictor.service.OddsService.OutcomeOdds;
import com.sportspredictor.service.PredictionService.PredictionResult;
import com.sportspredictor.service.StatsService.StatCategory;
import com.sportspredictor.service.StatsService.StatEntry;
import com.sportspredictor.service.StatsService.StatSplit;
import com.sportspredictor.service.StatsService.TeamStatsResult;
import com.sportspredictor.service.TrendService.RecordTrend;
import com.sportspredictor.service.TrendService.TrendResult;
import com.sportspredictor.service.WeatherService.WeatherResult;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link PredictionService}. */
@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    @Mock
    private StatsService statsService;

    @Mock
    private TrendService trendService;

    @Mock
    private OddsService oddsService;

    @Mock
    private InjuryService injuryService;

    @Mock
    private WeatherService weatherService;

    @Mock
    private PredictionLogRepository predictionLogRepository;

    @InjectMocks
    private PredictionService predictionService;

    private void setupDefaultMocks() {
        var stat = new StatEntry("points", 105.0, "105.0");
        var cat = new StatCategory("general", List.of(stat));
        var split = new StatSplit("Overall", List.of(cat));
        when(statsService.getTeamStats(eq("nba"), any(), any()))
                .thenReturn(new TeamStatsResult("nba", "1", List.of(split)));

        var record = new RecordTrend(6, 4, 0.6, "UP");
        when(trendService.analyzeTrends(eq("nba"), any(), any(), any()))
                .thenReturn(new TrendResult("nba", "1", "last_10", List.of(), record, "Summary"));

        var outcome = new OutcomeOdds("Team A", -110.0, null);
        var market = new MarketOdds("h2h", List.of(outcome));
        var book = new BookmakerOdds("dk", "DraftKings", List.of(market));
        var event = new EventOdds("evt-1", "Team A", "Team B", "2026-01-15", List.of(book));
        when(oddsService.getLiveOdds("nba", "evt-1", null))
                .thenReturn(new LiveOddsResult("nba", "evt-1", "h2h", List.of(event), 1));

        when(injuryService.getInjuryReport(eq("nba"), any()))
                .thenReturn(new InjuryReportResult("nba", null, List.of(), 0));
    }

    /** Tests for prediction generation. */
    @Nested
    class GeneratePrediction {

        @Test
        void returnsPredictionWithAllFields() {
            setupDefaultMocks();

            PredictionResult result =
                    predictionService.generatePrediction("nba", "evt-1", "1", "2", "MONEYLINE", null, null, null);

            assertThat(result.sport()).isEqualTo("nba");
            assertThat(result.eventId()).isEqualTo("evt-1");
            assertThat(result.team1Id()).isEqualTo("1");
            assertThat(result.team2Id()).isEqualTo("2");
            assertThat(result.predictionType()).isEqualTo("MONEYLINE");
            assertThat(result.predictedOutcome()).isNotBlank();
            assertThat(result.confidence()).isBetween(0.01, 0.99);
            assertThat(result.keyFactors()).isNotEmpty();
        }

        @Test
        void fetchesStatsForBothTeams() {
            setupDefaultMocks();

            predictionService.generatePrediction("nba", "evt-1", "1", "2", "MONEYLINE", null, null, null);

            verify(statsService).getTeamStats("nba", "1", null);
            verify(statsService).getTeamStats("nba", "2", null);
        }

        @Test
        void fetchesTrendsForBothTeams() {
            setupDefaultMocks();

            predictionService.generatePrediction("nba", "evt-1", "1", "2", "MONEYLINE", null, null, null);

            verify(trendService).analyzeTrends(eq("nba"), eq("1"), any(), any());
            verify(trendService).analyzeTrends(eq("nba"), eq("2"), any(), any());
        }

        @Test
        void logsPrediction() {
            setupDefaultMocks();

            predictionService.generatePrediction("nba", "evt-1", "1", "2", "MONEYLINE", null, null, null);

            verify(predictionLogRepository).save(any());
        }

        @Test
        void includesMatchupSummary() {
            setupDefaultMocks();

            PredictionResult result =
                    predictionService.generatePrediction("nba", "evt-1", "1", "2", "MONEYLINE", null, null, null);

            assertThat(result.matchup()).isNotNull();
            assertThat(result.matchup().team1()).isEqualTo("1");
            assertThat(result.matchup().team2()).isEqualTo("2");
        }

        @Test
        void includesTrendSummary() {
            setupDefaultMocks();

            PredictionResult result =
                    predictionService.generatePrediction("nba", "evt-1", "1", "2", "MONEYLINE", null, null, null);

            assertThat(result.trends()).isNotNull();
        }

        @Test
        void includesInjurySummary() {
            setupDefaultMocks();

            PredictionResult result =
                    predictionService.generatePrediction("nba", "evt-1", "1", "2", "MONEYLINE", null, null, null);

            assertThat(result.injuries()).isNotNull();
        }

        @Test
        void weatherNullWhenNoCoordsProvided() {
            setupDefaultMocks();

            PredictionResult result =
                    predictionService.generatePrediction("nba", "evt-1", "1", "2", "MONEYLINE", null, null, null);

            assertThat(result.weather()).isNotNull();
            assertThat(result.weather().temperatureCelsius()).isNull();
        }

        @Test
        void weatherIncludedWhenCoordsProvided() {
            setupDefaultMocks();
            when(weatherService.getWeatherForecast(34.0, -118.0, "2026-01-15"))
                    .thenReturn(new WeatherResult(
                            34.0,
                            -118.0,
                            "America/Los_Angeles",
                            List.of(new WeatherService.HourlyWeather("2026-01-15T19:00", 15.0, 0.0, 10.0, 50))));

            PredictionResult result = predictionService.generatePrediction(
                    "nba", "evt-1", "1", "2", "MONEYLINE", 34.0, -118.0, "2026-01-15");

            assertThat(result.weather().temperatureCelsius()).isEqualTo(15.0);
        }

        @Test
        void summaryContainsPredictedOutcome() {
            setupDefaultMocks();

            PredictionResult result =
                    predictionService.generatePrediction("nba", "evt-1", "1", "2", "MONEYLINE", null, null, null);

            assertThat(result.summary()).contains(result.predictedOutcome());
        }
    }
}
