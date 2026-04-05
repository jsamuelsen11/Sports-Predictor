package com.sportspredictor.mcpserver.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.mcpserver.service.TrendService;
import com.sportspredictor.mcpserver.service.TrendService.RecordTrend;
import com.sportspredictor.mcpserver.service.TrendService.TrendResult;
import com.sportspredictor.mcpserver.tool.TrendTool.TrendResponse;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link TrendTool}. */
@ExtendWith(MockitoExtension.class)
class TrendToolTest {

    @Mock
    private TrendService trendService;

    @InjectMocks
    private TrendTool trendTool;

    private static TrendResult buildTrendResult() {
        var record = new RecordTrend(7, 3, 0.7, "UP");
        return new TrendResult("nba", "1", "last_10", List.of(), record, "Trends summary");
    }

    /** Tests for {@link TrendTool#analyzeTrends}. */
    @Nested
    class AnalyzeTrends {

        @Test
        void delegatesToService() {
            when(trendService.analyzeTrends(eq("nba"), eq("1"), eq("last_5"), any()))
                    .thenReturn(buildTrendResult());

            trendTool.analyzeTrends("nba", "1", "last_5", null);

            verify(trendService).analyzeTrends(eq("nba"), eq("1"), eq("last_5"), any());
        }

        @Test
        void parsesCommaSeparatedMetrics() {
            when(trendService.analyzeTrends(eq("nba"), eq("1"), any(), eq(List.of("points", "rebounds"))))
                    .thenReturn(buildTrendResult());

            trendTool.analyzeTrends("nba", "1", null, "points,rebounds");

            verify(trendService).analyzeTrends(eq("nba"), eq("1"), any(), eq(List.of("points", "rebounds")));
        }

        @Test
        void nullMetricsPassedAsNull() {
            when(trendService.analyzeTrends("nba", "1", "last_10", null)).thenReturn(buildTrendResult());

            trendTool.analyzeTrends("nba", "1", "last_10", null);

            verify(trendService).analyzeTrends("nba", "1", "last_10", null);
        }

        @Test
        void wrapsServiceResult() {
            when(trendService.analyzeTrends(any(), any(), any(), any())).thenReturn(buildTrendResult());

            TrendResponse response = trendTool.analyzeTrends("nba", "1", "last_10", null);

            assertThat(response.sport()).isEqualTo("nba");
            assertThat(response.teamId()).isEqualTo("1");
            assertThat(response.window()).isEqualTo("last_10");
            assertThat(response.summary()).isEqualTo("Trends summary");
        }
    }
}
