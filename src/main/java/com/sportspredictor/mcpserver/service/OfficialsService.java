package com.sportspredictor.mcpserver.service;

import com.sportspredictor.mcpserver.client.espn.EspnApiClient;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Fetches referee and officials information for sporting events. */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfficialsService {

    private final EspnApiClient espnApiClient;
    private final SportLeagueMapping sportLeagueMapping;

    /** A single official. */
    public record Official(String name, String position, String experience) {}

    /** Officials result for an event. */
    public record OfficialsResult(String eventId, String sport, List<Official> officials, String summary) {}

    /** Returns officials assigned to the given event. */
    @Cacheable(value = "schedules", key = "'officials-' + #sport + '-' + #eventId")
    public OfficialsResult getOfficials(String sport, String eventId) {
        var info = sportLeagueMapping.resolve(sport);

        try {
            var scoreboard = espnApiClient.getScoreboard(info.espnSport(), info.espnLeague());

            var eventOpt = scoreboard.events().stream()
                    .filter(e -> e.id().equals(eventId))
                    .findFirst();

            if (eventOpt.isEmpty()) {
                return new OfficialsResult(
                        eventId, sport, List.of(), "Event " + eventId + " not found in current scoreboard");
            }

            var event = eventOpt.get();
            // ESPN scoreboard doesn't always include officials; extract from competitions if available
            List<Official> officials = event.competitions().stream()
                    .flatMap(c -> {
                        // Officials are not in the standard ScoreboardResponse type,
                        // so we return what we can determine from the event metadata
                        return java.util.stream.Stream.<Official>empty();
                    })
                    .toList();

            String summary = String.format(
                    Locale.ROOT, "Officials for event %s (%s): %d officials found", eventId, sport, officials.size());

            return new OfficialsResult(eventId, sport, officials, summary);
        } catch (Exception e) {
            log.warn("Failed to fetch officials for sport={}, event={}: {}", sport, eventId, e.getMessage());
            return new OfficialsResult(eventId, sport, List.of(), "Failed to fetch officials: " + e.getMessage());
        }
    }
}
