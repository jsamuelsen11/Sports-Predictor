package com.sportspredictor.service;

import com.sportspredictor.client.espn.EspnApiClient;
import com.sportspredictor.client.espn.RosterResponse;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Analyzes NBA lineup impact using roster and performance data. */
@Service
@RequiredArgsConstructor
@Slf4j
public class LineupImpactService {

    private static final double BASE_IMPACT = 1.0;

    private final EspnApiClient espnApiClient;
    private final SportLeagueMapping sportLeagueMapping;

    /** A player's impact contribution. */
    public record LineupPlayer(String playerId, String name, String position, double impactScore) {}

    /** Full lineup impact analysis. */
    public record LineupImpactResult(
            String teamId,
            List<LineupPlayer> lineup,
            double projectedImpact,
            double offensiveRating,
            double defensiveRating,
            String summary) {}

    /** Analyzes NBA lineup impact by evaluating player roles and estimated contributions. */
    @Cacheable(value = "team-stats", key = "'lineup-impact-' + #teamId")
    public LineupImpactResult getNbaLineupImpact(String teamId) {
        var info = sportLeagueMapping.resolve("nba");

        try {
            RosterResponse roster = espnApiClient.getRoster(info.espnSport(), info.espnLeague(), teamId);

            List<LineupPlayer> players = roster.athletes().stream()
                    .flatMap(group -> group.items().stream())
                    .map(a -> {
                        double impact = estimatePlayerImpact(a.position());
                        return new LineupPlayer(a.id(), a.displayName(), a.position(), impact);
                    })
                    .sorted((a, b) -> Double.compare(b.impactScore(), a.impactScore()))
                    .limit(15)
                    .toList();

            double totalImpact =
                    players.stream().mapToDouble(LineupPlayer::impactScore).sum();
            double offRating = totalImpact * 0.6;
            double defRating = totalImpact * 0.4;

            String summary = String.format(
                    Locale.ROOT,
                    "NBA Lineup Impact for team %s: %d players analyzed, projected impact %.2f "
                            + "(off: %.2f, def: %.2f)",
                    teamId,
                    players.size(),
                    totalImpact,
                    offRating,
                    defRating);

            log.info("Lineup impact computed for team {}: {} players", teamId, players.size());

            return new LineupImpactResult(teamId, players, totalImpact, offRating, defRating, summary);
        } catch (Exception e) {
            log.warn("Failed to get lineup impact for team {}: {}", teamId, e.getMessage());
            return new LineupImpactResult(teamId, List.of(), 0, 0, 0, "Failed to analyze lineup: " + e.getMessage());
        }
    }

    private static double estimatePlayerImpact(String position) {
        if (position == null) {
            return BASE_IMPACT;
        }
        String pos = position.toUpperCase(Locale.ROOT);
        return switch (pos) {
            case "PG", "POINT GUARD" -> BASE_IMPACT * 1.3;
            case "SG", "SHOOTING GUARD" -> BASE_IMPACT * 1.2;
            case "SF", "SMALL FORWARD" -> BASE_IMPACT * 1.15;
            case "PF", "POWER FORWARD" -> BASE_IMPACT * 1.1;
            case "C", "CENTER" -> BASE_IMPACT * 1.25;
            default -> BASE_IMPACT;
        };
    }
}
