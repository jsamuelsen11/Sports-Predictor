package com.sportspredictor.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.service.DailyRankingService;
import com.sportspredictor.service.DailyRankingService.DailyCardResult;
import com.sportspredictor.tool.DailyRankingTool.DailyCardResponse;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link DailyRankingTool}. */
@ExtendWith(MockitoExtension.class)
class DailyRankingToolTest {

    @Mock
    private DailyRankingService dailyRankingService;

    @InjectMocks
    private DailyRankingTool dailyRankingTool;

    private static DailyCardResult buildDailyCard() {
        return new DailyCardResult("nba", "20260115", List.of(), 5, 0, "Daily card summary");
    }

    /** Tests for {@link DailyRankingTool#rankTodaysPlays}. */
    @Nested
    class RankTodaysPlays {

        @Test
        void delegatesToService() {
            when(dailyRankingService.rankTodaysPlays("nba", 0.6, 3.0)).thenReturn(buildDailyCard());

            dailyRankingTool.rankTodaysPlays("nba", 0.6, 3.0);

            verify(dailyRankingService).rankTodaysPlays("nba", 0.6, 3.0);
        }

        @Test
        void wrapsServiceResult() {
            when(dailyRankingService.rankTodaysPlays("nba", null, null)).thenReturn(buildDailyCard());

            DailyCardResponse response = dailyRankingTool.rankTodaysPlays("nba", null, null);

            assertThat(response.sport()).isEqualTo("nba");
            assertThat(response.totalGames()).isEqualTo(5);
            assertThat(response.summary()).isEqualTo("Daily card summary");
        }
    }
}
