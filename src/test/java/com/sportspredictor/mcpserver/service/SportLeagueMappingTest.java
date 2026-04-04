package com.sportspredictor.mcpserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SportLeagueMappingTest {

    private final SportLeagueMapping mapping = new SportLeagueMapping();

    @Nested
    class Resolve {

        @ParameterizedTest
        @ValueSource(strings = {"nfl", "nba", "mlb", "nhl", "epl", "laliga", "bundesliga", "seriea", "mls", "ucl"})
        void resolvesAllSupportedSports(String sport) {
            var info = mapping.resolve(sport);
            assertThat(info).isNotNull();
            assertThat(info.key()).isEqualTo(sport);
            assertThat(info.espnSport()).isNotBlank();
            assertThat(info.espnLeague()).isNotBlank();
            assertThat(info.oddsApiKey()).isNotBlank();
            assertThat(info.displayName()).isNotBlank();
        }

        @Test
        void resolvesNflCorrectly() {
            var info = mapping.resolve("nfl");
            assertThat(info.espnSport()).isEqualTo("football");
            assertThat(info.espnLeague()).isEqualTo("nfl");
            assertThat(info.oddsApiKey()).isEqualTo("americanfootball_nfl");
        }

        @Test
        void resolvesEplCorrectly() {
            var info = mapping.resolve("epl");
            assertThat(info.espnSport()).isEqualTo("soccer");
            assertThat(info.espnLeague()).isEqualTo("eng.1");
            assertThat(info.oddsApiKey()).isEqualTo("soccer_epl");
        }

        @Test
        void isCaseInsensitive() {
            var info = mapping.resolve("NFL");
            assertThat(info.key()).isEqualTo("nfl");
        }

        @Test
        void throwsForUnknownSport() {
            assertThatThrownBy(() -> mapping.resolve("cricket"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown sport key: cricket");
        }
    }

    @Nested
    class AllLeagues {

        @Test
        void returnsAllLeagues() {
            assertThat(mapping.allLeagues()).hasSize(13);
        }

        @Test
        void containsExpectedSports() {
            var keys = mapping.allLeagues().stream()
                    .map(SportLeagueMapping.LeagueInfo::key)
                    .toList();
            assertThat(keys).contains("nfl", "nba", "mlb", "nhl", "epl", "mma", "golf", "tennis");
        }
    }
}
