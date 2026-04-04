package com.sportspredictor.mcpserver.tool;

import com.sportspredictor.mcpserver.service.NewsFeedService;
import com.sportspredictor.mcpserver.service.NewsFeedService.NewsFeedResult;
import com.sportspredictor.mcpserver.service.NewsFeedService.NewsItem;
import com.sportspredictor.mcpserver.service.OfficialsService;
import com.sportspredictor.mcpserver.service.OfficialsService.Official;
import com.sportspredictor.mcpserver.service.OfficialsService.OfficialsResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tools for data ingestion: officials and news feeds. */
@Service
@RequiredArgsConstructor
public class IngestionTool {

    private final OfficialsService officialsService;
    private final NewsFeedService newsFeedService;

    /** Response for get_referee_officials. */
    public record OfficialsResponse(String eventId, String sport, List<Official> officials, String summary) {}

    /** Response for get_news_feed. */
    public record NewsFeedResponse(String sport, String teamId, List<NewsItem> items, int count, String summary) {}

    /** Returns assigned officials for an upcoming game. */
    @Tool(
            name = "get_referee_officials",
            description = "Get assigned referee/officials for an upcoming game."
                    + " Officials affect foul rates, penalty tendencies, and game pace")
    public OfficialsResponse getRefereeOfficials(
            @ToolParam(description = "Sport key (e.g., nba, nfl)") String sport,
            @ToolParam(description = "Event ID") String eventId) {
        OfficialsResult r = officialsService.getOfficials(sport, eventId);
        return new OfficialsResponse(r.eventId(), r.sport(), r.officials(), r.summary());
    }

    /** Returns aggregated sports news for a team. */
    @Tool(
            name = "get_news_feed",
            description = "Get aggregated sports news for a team or sport."
                    + " Useful for injury updates, trades, and team news affecting predictions")
    public NewsFeedResponse getNewsFeed(
            @ToolParam(description = "Sport key (e.g., nba, nfl)") String sport,
            @ToolParam(description = "Team name or ID filter (optional)", required = false) String teamId) {
        NewsFeedResult r = newsFeedService.getNewsFeed(sport, teamId);
        return new NewsFeedResponse(r.sport(), r.teamId(), r.items(), r.count(), r.summary());
    }
}
