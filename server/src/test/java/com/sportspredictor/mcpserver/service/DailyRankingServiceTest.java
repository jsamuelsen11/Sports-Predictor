package com.sportspredictor.mcpserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sportspredictor.mcpserver.service.DailyRankingService.DailyCardResult;
import com.sportspredictor.mcpserver.service.OddsComparisonService.BestLine;
import com.sportspredictor.mcpserver.service.OddsComparisonService.OddsComparison;
import com.sportspredictor.mcpserver.service.PredictionService.PredictionResult;
import com.sportspredictor.mcpserver.service.ScheduleService.GameInfo;
import com.sportspredictor.mcpserver.service.ScheduleService.ScheduleResult;
import com.sportspredictor.mcpserver.service.ScheduleService.TeamInfo;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link DailyRankingService}. */
@ExtendWith(MockitoExtension.class)
class DailyRankingServiceTest {

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private PredictionService predictionService;

    @Mock
    private OddsComparisonService oddsComparisonService;

    @InjectMocks
    private DailyRankingService dailyRankingService;

    private static ScheduleResult buildSchedule(int gameCount) {
        List<GameInfo> games = java.util.stream.IntStream.range(0, gameCount)
                .mapToObj(i -> {
                    var home = new TeamInfo("h" + i, "Home " + i, "H" + i, null, false);
                    var away = new TeamInfo("a" + i, "Away " + i, "A" + i, null, false);
                    return new GameInfo(
                            "evt-" + i,
                            "2026-01-15",
                            "Home " + i + " vs Away " + i,
                            "H" + i + " @ A" + i,
                            List.of(home, away),
                            false);
                })
                .toList();
        return new ScheduleResult("nba", "20260115", games, games.size());
    }

    private static PredictionResult buildPrediction(String eventId, double confidence) {
        return new PredictionResult(
                "pred-1",
                eventId,
                "nba",
                "h0",
                "a0",
                "MONEYLINE",
                "h0",
                confidence,
                confidence,
                List.of("Factor"),
                null,
                null,
                null,
                null,
                null,
                "Summary");
    }

    private static OddsComparison buildOddsComparison(String eventId) {
        var bestHome = new BestLine("Home 0", "DraftKings", -110, 0.52);
        var bestAway = new BestLine("Away 0", "FanDuel", 100, 0.50);
        return new OddsComparison(eventId, "Home 0", "Away 0", List.of(), bestHome, bestAway, "Summary");
    }

    /** Tests for {@link DailyRankingService#rankTodaysPlays}. */
    @Nested
    class RankTodaysPlays {

        @Test
        void returnsRankedPlays() {
            when(scheduleService.getTodaySchedule("nba")).thenReturn(buildSchedule(2));
            when(predictionService.generatePrediction(eq("nba"), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(buildPrediction("evt-0", 0.65));
            when(oddsComparisonService.compareOdds(eq("nba"), any(), eq("h2h")))
                    .thenReturn(buildOddsComparison("evt-0"));

            DailyCardResult result = dailyRankingService.rankTodaysPlays("nba", null, null);

            assertThat(result.rankedPlays()).isNotEmpty();
            assertThat(result.totalGames()).isEqualTo(2);
        }

        @Test
        void filtersbyMinConfidence() {
            when(scheduleService.getTodaySchedule("nba")).thenReturn(buildSchedule(1));
            when(predictionService.generatePrediction(eq("nba"), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(buildPrediction("evt-0", 0.45));

            DailyCardResult result = dailyRankingService.rankTodaysPlays("nba", 0.5, null);

            assertThat(result.rankedPlays()).isEmpty();
        }

        @Test
        void ranksAreAssignedSequentially() {
            when(scheduleService.getTodaySchedule("nba")).thenReturn(buildSchedule(3));
            when(predictionService.generatePrediction(eq("nba"), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(buildPrediction("evt-0", 0.7));
            when(oddsComparisonService.compareOdds(eq("nba"), any(), eq("h2h")))
                    .thenReturn(buildOddsComparison("evt-0"));

            DailyCardResult result = dailyRankingService.rankTodaysPlays("nba", null, null);

            for (int i = 0; i < result.rankedPlays().size(); i++) {
                assertThat(result.rankedPlays().get(i).rank()).isEqualTo(i + 1);
            }
        }

        @Test
        void emptyScheduleReturnsEmptyCard() {
            when(scheduleService.getTodaySchedule("nba"))
                    .thenReturn(new ScheduleResult("nba", "20260115", List.of(), 0));

            DailyCardResult result = dailyRankingService.rankTodaysPlays("nba", null, null);

            assertThat(result.rankedPlays()).isEmpty();
            assertThat(result.totalGames()).isEqualTo(0);
        }

        @Test
        void summaryContainsSport() {
            when(scheduleService.getTodaySchedule("nba")).thenReturn(buildSchedule(1));
            when(predictionService.generatePrediction(eq("nba"), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(buildPrediction("evt-0", 0.65));
            when(oddsComparisonService.compareOdds(eq("nba"), any(), eq("h2h")))
                    .thenReturn(buildOddsComparison("evt-0"));

            DailyCardResult result = dailyRankingService.rankTodaysPlays("nba", null, null);

            assertThat(result.summary()).contains("NBA");
        }
    }
}
