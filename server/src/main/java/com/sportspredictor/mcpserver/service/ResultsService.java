package com.sportspredictor.mcpserver.service;

import com.sportspredictor.mcpserver.client.espn.EspnApiClient;
import com.sportspredictor.mcpserver.client.espn.ScoreboardResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Fetches completed game results from ESPN with caching and filtering. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResultsService {

    private final EspnApiClient espnApiClient;
    private final SportLeagueMapping sportLeagueMapping;

    /** Game results query result. */
    public record GameResultsResult(String sport, String date, List<GameResult> results, int gameCount) {}

    /** A completed game result. */
    public record GameResult(String id, String date, String name, List<TeamResult> teams) {}

    /** A team's result in a completed game. */
    public record TeamResult(String id, String displayName, String abbreviation, String score, boolean winner) {}

    /** Returns cached completed game results for the given sport with optional date and event filtering. */
    @Cacheable(value = "schedules", key = "'results-' + #sport + '-' + #date + '-' + #eventId")
    public GameResultsResult getGameResults(String sport, String date, String eventId) {
        var info = sportLeagueMapping.resolve(sport);

        try {
            ScoreboardResponse scoreboard;
            if (date != null && !date.isBlank()) {
                scoreboard = espnApiClient.getSchedule(info.espnSport(), info.espnLeague(), date);
            } else {
                scoreboard = espnApiClient.getScoreboard(info.espnSport(), info.espnLeague());
            }

            List<GameResult> results = scoreboard.events().stream()
                    .filter(e -> eventId == null || eventId.isBlank() || e.id().equals(eventId))
                    .filter(this::isCompleted)
                    .map(this::toGameResult)
                    .toList();

            return new GameResultsResult(sport, date, results, results.size());
        } catch (Exception e) {
            log.warn("Failed to fetch game results for sport={}: {}", sport, e.getMessage());
            return new GameResultsResult(sport, date, List.of(), 0);
        }
    }

    private boolean isCompleted(ScoreboardResponse.Event event) {
        return event.competitions().stream()
                .anyMatch(c -> c.status() != null
                        && c.status().type() != null
                        && c.status().type().completed());
    }

    private GameResult toGameResult(ScoreboardResponse.Event event) {
        List<TeamResult> teams = event.competitions().stream()
                .flatMap(c -> c.competitors().stream())
                .map(comp -> new TeamResult(
                        comp.team().id(),
                        comp.team().displayName(),
                        comp.team().abbreviation(),
                        comp.score(),
                        comp.winner()))
                .toList();
        return new GameResult(event.id(), event.date(), event.name(), teams);
    }
}
