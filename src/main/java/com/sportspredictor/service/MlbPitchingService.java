package com.sportspredictor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** MLB pitching matchup analysis: starting pitcher comparison with relevant splits. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MlbPitchingService {

    /** A single pitcher's stats. */
    public record PitcherStats(String name, double era, double whip, double k9, double bb9, String recentForm) {}

    /** Pitching matchup result. */
    public record PitchingMatchupResult(
            String eventId, PitcherStats homePitcher, PitcherStats awayPitcher, String summary) {}

    /** Gets the pitching matchup for an MLB event. */
    public PitchingMatchupResult getPitchingMatchup(String sport, String eventId) {
        log.info("Getting pitching matchup sport={} event={}", sport, eventId);

        PitcherStats home = new PitcherStats("Home Starter", 3.45, 1.12, 9.8, 2.5, "W-L-W-W-L");
        PitcherStats away = new PitcherStats("Away Starter", 4.12, 1.28, 8.2, 3.1, "L-W-L-W-W");

        return new PitchingMatchupResult(
                eventId,
                home,
                away,
                String.format(
                        "Matchup: %s (%.2f ERA) vs %s (%.2f ERA).", home.name(), home.era(), away.name(), away.era()));
    }
}
