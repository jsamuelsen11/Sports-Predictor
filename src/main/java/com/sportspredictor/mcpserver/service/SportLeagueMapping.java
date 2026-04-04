package com.sportspredictor.mcpserver.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Maps user-facing sport keys to API-specific identifiers for ESPN, OddsAPI, and display. */
@Service
public class SportLeagueMapping {

    /** API-specific identifiers for a league. */
    public record LeagueInfo(String key, String espnSport, String espnLeague, String oddsApiKey, String displayName) {}

    private static final Map<String, LeagueInfo> LEAGUES = Map.ofEntries(
            // NFL
            Map.entry("nfl", new LeagueInfo("nfl", "football", "nfl", "americanfootball_nfl", "NFL")),
            // NBA
            Map.entry("nba", new LeagueInfo("nba", "basketball", "nba", "basketball_nba", "NBA")),
            // MLB
            Map.entry("mlb", new LeagueInfo("mlb", "baseball", "mlb", "baseball_mlb", "MLB")),
            // NHL
            Map.entry("nhl", new LeagueInfo("nhl", "hockey", "nhl", "icehockey_nhl", "NHL")),
            // Soccer — EPL
            Map.entry("epl", new LeagueInfo("epl", "soccer", "eng.1", "soccer_epl", "English Premier League")),
            // Soccer — La Liga
            Map.entry("laliga", new LeagueInfo("laliga", "soccer", "esp.1", "soccer_spain_la_liga", "La Liga")),
            // Soccer — Bundesliga
            Map.entry(
                    "bundesliga",
                    new LeagueInfo("bundesliga", "soccer", "ger.1", "soccer_germany_bundesliga", "Bundesliga")),
            // Soccer — Serie A
            Map.entry("seriea", new LeagueInfo("seriea", "soccer", "ita.1", "soccer_italy_serie_a", "Serie A")),
            // Soccer — MLS
            Map.entry("mls", new LeagueInfo("mls", "soccer", "usa.1", "soccer_usa_mls", "MLS")),
            // Soccer — UCL
            Map.entry(
                    "ucl",
                    new LeagueInfo(
                            "ucl", "soccer", "uefa.champions", "soccer_uefa_champs_league", "UEFA Champions League")),
            // MMA/UFC
            Map.entry("mma", new LeagueInfo("mma", "mma", "ufc", "mma_mixed_martial_arts", "UFC/MMA")),
            // Golf — PGA
            Map.entry("golf", new LeagueInfo("golf", "golf", "pga", "golf_masters_tournament_winner", "PGA Golf")),
            // Tennis — ATP
            Map.entry("tennis", new LeagueInfo("tennis", "tennis", "atp", "tennis_atp_us_open", "ATP Tennis")));

    /**
     * Resolves a user-facing sport key to API identifiers.
     *
     * @throws IllegalArgumentException if the sport key is unknown
     */
    public LeagueInfo resolve(String sport) {
        LeagueInfo info = LEAGUES.get(sport.toLowerCase(Locale.ROOT));
        if (info == null) {
            throw new IllegalArgumentException("Unknown sport key: " + sport + ". Supported: " + LEAGUES.keySet());
        }
        return info;
    }

    /** Returns all supported leagues. */
    public List<LeagueInfo> allLeagues() {
        return List.copyOf(LEAGUES.values());
    }
}
