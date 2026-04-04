package com.sportspredictor.mcpserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sportspredictor.mcpserver.client.espn.EspnApiClient;
import com.sportspredictor.mcpserver.client.espn.ScoreboardResponse;
import com.sportspredictor.mcpserver.service.NewsFeedService.NewsFeedResult;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link NewsFeedService}. */
@ExtendWith(MockitoExtension.class)
class NewsFeedServiceTest {

    @Mock
    private EspnApiClient espnApiClient;

    @Mock
    private SportLeagueMapping sportLeagueMapping;

    @InjectMocks
    private NewsFeedService newsFeedService;

    private static SportLeagueMapping.LeagueInfo nbaInfo() {
        return new SportLeagueMapping.LeagueInfo("nba", "basketball", "nba", "basketball_nba", "NBA");
    }

    private static SportLeagueMapping.LeagueInfo nflInfo() {
        return new SportLeagueMapping.LeagueInfo("nfl", "football", "nfl", "americanfootball_nfl", "NFL");
    }

    private static ScoreboardResponse buildScoreboardWithTwoEvents() {
        ScoreboardResponse.StatusType status = new ScoreboardResponse.StatusType("Final", true);

        ScoreboardResponse.Team lakersTeam = new ScoreboardResponse.Team("lakers", "Los Angeles Lakers", "LAL");
        ScoreboardResponse.Team celticsTeam = new ScoreboardResponse.Team("celtics", "Boston Celtics", "BOS");
        ScoreboardResponse.Competitor lakersComp = new ScoreboardResponse.Competitor("1", lakersTeam, "110", true);
        ScoreboardResponse.Competitor celticsComp = new ScoreboardResponse.Competitor("2", celticsTeam, "105", false);

        ScoreboardResponse.Competition comp1 = new ScoreboardResponse.Competition(
                "comp-1", "2026-03-29", List.of(lakersComp, celticsComp), new ScoreboardResponse.Status(status));
        ScoreboardResponse.Event event1 =
                new ScoreboardResponse.Event("evt-1", "2026-03-29", "Lakers vs Celtics", "LAL vs BOS", List.of(comp1));

        ScoreboardResponse.Team heatTeam = new ScoreboardResponse.Team("heat", "Miami Heat", "MIA");
        ScoreboardResponse.Team buksTeam = new ScoreboardResponse.Team("bucks", "Milwaukee Bucks", "MIL");
        ScoreboardResponse.Competitor heatComp = new ScoreboardResponse.Competitor("3", heatTeam, "98", false);
        ScoreboardResponse.Competitor bucksComp = new ScoreboardResponse.Competitor("4", buksTeam, "115", true);

        ScoreboardResponse.Competition comp2 = new ScoreboardResponse.Competition(
                "comp-2", "2026-03-29", List.of(heatComp, bucksComp), new ScoreboardResponse.Status(status));
        ScoreboardResponse.Event event2 =
                new ScoreboardResponse.Event("evt-2", "2026-03-29", "Heat vs Bucks", "MIA vs MIL", List.of(comp2));

        return new ScoreboardResponse(List.of(event1, event2));
    }

    /** Tests for {@link NewsFeedService#getNewsFeed}. */
    @Nested
    class GetNewsFeed {

        @Test
        void returnsAllEventsWhenNoTeamFilter() {
            when(sportLeagueMapping.resolve("nba")).thenReturn(nbaInfo());
            when(espnApiClient.getScoreboard(eq("basketball"), eq("nba"))).thenReturn(buildScoreboardWithTwoEvents());

            NewsFeedResult result = newsFeedService.getNewsFeed("nba", null);

            assertThat(result.sport()).isEqualTo("nba");
            assertThat(result.items()).hasSize(2);
            assertThat(result.count()).isEqualTo(2);
            assertThat(result.summary()).contains("NBA");
        }

        @Test
        void filtersItemsByTeamIdWhenProvided() {
            when(sportLeagueMapping.resolve("nba")).thenReturn(nbaInfo());
            when(espnApiClient.getScoreboard(eq("basketball"), eq("nba"))).thenReturn(buildScoreboardWithTwoEvents());

            // Filter to Lakers — only the Lakers vs Celtics event should match
            NewsFeedResult result = newsFeedService.getNewsFeed("nba", "Los Angeles Lakers");

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().getFirst().headline()).isEqualTo("Lakers vs Celtics");
        }

        @Test
        void newsItemsContainHeadlineAndDate() {
            when(sportLeagueMapping.resolve("nba")).thenReturn(nbaInfo());
            when(espnApiClient.getScoreboard(eq("basketball"), eq("nba"))).thenReturn(buildScoreboardWithTwoEvents());

            NewsFeedResult result = newsFeedService.getNewsFeed("nba", null);

            result.items().forEach(item -> {
                assertThat(item.headline()).isNotBlank();
                assertThat(item.published()).isNotBlank();
            });
        }

        @Test
        void returnsGracefulResultWhenEspnClientThrows() {
            when(sportLeagueMapping.resolve("nba")).thenReturn(nbaInfo());
            when(espnApiClient.getScoreboard(eq("basketball"), eq("nba")))
                    .thenThrow(new RuntimeException("ESPN timeout"));

            NewsFeedResult result = newsFeedService.getNewsFeed("nba", null);

            assertThat(result.items()).isEmpty();
            assertThat(result.count()).isEqualTo(0);
            assertThat(result.summary()).containsIgnoringCase("failed");
        }

        @Test
        void returnsEmptyFeedWhenScoreboardHasNoEvents() {
            when(sportLeagueMapping.resolve("nfl")).thenReturn(nflInfo());
            when(espnApiClient.getScoreboard(eq("football"), eq("nfl"))).thenReturn(new ScoreboardResponse(List.of()));

            NewsFeedResult result = newsFeedService.getNewsFeed("nfl", null);

            assertThat(result.items()).isEmpty();
            assertThat(result.count()).isEqualTo(0);
        }

        @Test
        void summaryContainsSportName() {
            when(sportLeagueMapping.resolve("nba")).thenReturn(nbaInfo());
            when(espnApiClient.getScoreboard(eq("basketball"), eq("nba"))).thenReturn(buildScoreboardWithTwoEvents());

            NewsFeedResult result = newsFeedService.getNewsFeed("nba", null);

            assertThat(result.summary()).containsIgnoringCase("NBA");
        }
    }
}
