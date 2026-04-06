package com.sportspredictor.mcpserver.service;

import com.sportspredictor.mcpserver.client.espn.ScoreboardResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Fetches game schedules from ESPN with caching and date formatting. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleService {

    private static final DateTimeFormatter ESPN_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int WEEK_DAYS = 7;

    private final com.sportspredictor.mcpserver.client.espn.EspnApiClient espnApiClient;
    private final SportLeagueMapping sportLeagueMapping;

    /** A scheduled game with teams and status. */
    public record GameInfo(
            String id, String date, String name, String shortName, List<TeamInfo> teams, boolean completed) {}

    /** A team in a game. */
    public record TeamInfo(String id, String displayName, String abbreviation, String score, boolean winner) {}

    /** Result record for schedule queries. */
    public record ScheduleResult(String sport, String dateRange, List<GameInfo> games, int gameCount) {}

    /** Returns cached game schedule for the given sport with optional date range and team filtering. */
    @Cacheable(value = "schedules", key = "#sport + '-' + #dateRange + '-' + #team")
    public ScheduleResult getGameSchedule(String sport, String dateRange, String team) {
        var info = sportLeagueMapping.resolve(sport);

        try {
            ScoreboardResponse scoreboard;
            if (dateRange != null && !dateRange.isBlank()) {
                scoreboard = espnApiClient.getSchedule(info.espnSport(), info.espnLeague(), dateRange);
            } else {
                scoreboard = espnApiClient.getScoreboard(info.espnSport(), info.espnLeague());
            }

            List<GameInfo> games = scoreboard.events().stream()
                    .map(this::toGameInfo)
                    .filter(g -> team == null || team.isBlank() || matchesTeam(g, team))
                    .toList();

            return new ScheduleResult(sport, dateRange, games, games.size());
        } catch (Exception e) {
            log.warn("Failed to fetch schedule for sport={}: {}", sport, e.getMessage());
            return new ScheduleResult(sport, dateRange, List.of(), 0);
        }
    }

    /** Returns the game schedule for today for the given sport. */
    public ScheduleResult getTodaySchedule(String sport) {
        String today = LocalDate.now(ZoneId.systemDefault()).format(ESPN_DATE_FORMAT);
        return getGameSchedule(sport, today, null);
    }

    /** Returns the game schedule for the next seven days for the given sport. */
    public ScheduleResult getWeekSchedule(String sport) {
        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        String range =
                now.format(ESPN_DATE_FORMAT) + "-" + now.plusDays(WEEK_DAYS).format(ESPN_DATE_FORMAT);
        return getGameSchedule(sport, range, null);
    }

    private GameInfo toGameInfo(ScoreboardResponse.Event event) {
        boolean completed = event.competitions().stream()
                .anyMatch(c -> c.status() != null
                        && c.status().type() != null
                        && c.status().type().completed());

        List<TeamInfo> teams = event.competitions().stream()
                .flatMap(c -> c.competitors().stream())
                .map(comp -> new TeamInfo(
                        comp.team().id(),
                        comp.team().displayName(),
                        comp.team().abbreviation(),
                        comp.score(),
                        comp.winner()))
                .toList();

        return new GameInfo(event.id(), event.date(), event.name(), event.shortName(), teams, completed);
    }

    private boolean matchesTeam(GameInfo game, String team) {
        return game.teams().stream()
                .anyMatch(t -> t.displayName().equalsIgnoreCase(team)
                        || t.abbreviation().equalsIgnoreCase(team));
    }
}
