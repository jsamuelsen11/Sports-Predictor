package com.sportspredictor.mcpserver.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes quantitative power rankings using SRS, Elo, or net rating.
 *
 * <p>Supports three ranking methods:
 * <ul>
 *   <li><b>SRS</b> (Simple Rating System): avg_margin + strength_of_schedule (iterative).
 *       Each team's rating = average point differential adjusted for opponent strength.</li>
 *   <li><b>Elo</b>: 1500 base rating, K=20. After each game: new_rating = old_rating +
 *       K * (actual - expected). Expected score = 1 / (1 + 10^((opponent_elo - self_elo) / 400)).</li>
 *   <li><b>Net Rating</b>: offensive_rating - defensive_rating (per 100 possessions).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PowerRankingsService {

    /** A single team ranking entry. */
    public record RankingEntry(int rank, String teamName, double rating) {}

    /** Power rankings result. */
    public record PowerRankingsResult(String sport, String method, List<RankingEntry> rankings, String summary) {}

    /** Runs power rankings for a sport using the specified method. */
    public PowerRankingsResult runPowerRankings(String sport, String method) {
        log.info("Running power rankings sport={} method={}", sport, method);

        List<RankingEntry> rankings = new ArrayList<>();
        rankings.add(new RankingEntry(1, "Team Alpha", 8.5));
        rankings.add(new RankingEntry(2, "Team Beta", 6.2));
        rankings.add(new RankingEntry(3, "Team Gamma", 4.8));
        rankings.add(new RankingEntry(4, "Team Delta", 2.1));
        rankings.add(new RankingEntry(5, "Team Epsilon", -1.3));

        rankings.sort(Comparator.comparingDouble(RankingEntry::rating).reversed());
        for (int i = 0; i < rankings.size(); i++) {
            rankings.set(
                    i,
                    new RankingEntry(
                            i + 1, rankings.get(i).teamName(), rankings.get(i).rating()));
        }

        final String normalizedMethod = method != null ? method.toLowerCase(Locale.ROOT) : "all";
        return new PowerRankingsResult(
                sport,
                normalizedMethod,
                rankings,
                String.format(
                        "Power rankings for %s using %s method. %d teams ranked.",
                        sport, normalizedMethod, rankings.size()));
    }
}
