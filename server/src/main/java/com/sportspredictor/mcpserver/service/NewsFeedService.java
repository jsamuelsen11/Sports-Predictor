package com.sportspredictor.mcpserver.service;

import com.sportspredictor.mcpserver.client.espn.EspnApiClient;
import com.sportspredictor.mcpserver.client.espn.ScoreboardResponse;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Aggregates sports news from ESPN for teams and players. */
@Service
@RequiredArgsConstructor
@Slf4j
public class NewsFeedService {

    private final EspnApiClient espnApiClient;
    private final SportLeagueMapping sportLeagueMapping;

    /** A single news item. */
    public record NewsItem(String headline, String description, String published) {}

    /** News feed result. */
    public record NewsFeedResult(String sport, String teamId, List<NewsItem> items, int count, String summary) {}

    /** Returns aggregated news for a sport, optionally filtered to a team. */
    @Cacheable(value = "schedules", key = "'news-' + #sport + '-' + #teamId")
    public NewsFeedResult getNewsFeed(String sport, String teamId) {
        var info = sportLeagueMapping.resolve(sport);

        try {
            ScoreboardResponse scoreboard = espnApiClient.getScoreboard(info.espnSport(), info.espnLeague());

            // Extract headlines from scoreboard events as a news proxy
            List<NewsItem> items = scoreboard.events().stream()
                    .filter(e -> teamId == null
                            || teamId.isBlank()
                            || e.competitions().stream()
                                    .anyMatch(c -> c.competitors().stream().anyMatch(comp -> comp.team()
                                            .displayName()
                                            .toLowerCase(Locale.ROOT)
                                            .contains(teamId.toLowerCase(Locale.ROOT)))))
                    .map(e -> new NewsItem(e.name(), e.shortName() != null ? e.shortName() : e.name(), e.date()))
                    .toList();

            String summary = String.format(
                    Locale.ROOT,
                    "News for %s%s: %d items",
                    sport.toUpperCase(Locale.ROOT),
                    teamId != null ? " (" + teamId + ")" : "",
                    items.size());

            return new NewsFeedResult(sport, teamId, items, items.size(), summary);
        } catch (Exception e) {
            log.warn("Failed to fetch news for sport={}, team={}: {}", sport, teamId, e.getMessage());
            return new NewsFeedResult(sport, teamId, List.of(), 0, "Failed to fetch news: " + e.getMessage());
        }
    }
}
