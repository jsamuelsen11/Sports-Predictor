package com.sportspredictor.mcpserver.config;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Externalized API client configuration, bound to {@code app.client} properties. */
@ConfigurationProperties(prefix = "app.client")
public record ClientProperties(
        OddsApiProps oddsApi, ApiSportsProps apiSports, EspnProps espn, OpenMeteoProps openMeteo) {

    private static final String ODDS_API_BASE_URL = "https://api.the-odds-api.com/v4";
    private static final String API_SPORTS_BASE_URL = "https://v3.football.api-sports.io";
    private static final String ESPN_BASE_URL = "https://site.api.espn.com";
    private static final String OPEN_METEO_BASE_URL = "https://api.open-meteo.com/v1";

    /** Creates properties with default sub-configurations when values are not specified. */
    public ClientProperties {
        oddsApi = Objects.requireNonNullElse(oddsApi, new OddsApiProps(ODDS_API_BASE_URL, ""));
        apiSports = Objects.requireNonNullElse(apiSports, new ApiSportsProps(API_SPORTS_BASE_URL, ""));
        espn = Objects.requireNonNullElse(espn, new EspnProps(ESPN_BASE_URL));
        openMeteo = Objects.requireNonNullElse(openMeteo, new OpenMeteoProps(OPEN_METEO_BASE_URL));
    }

    /** The Odds API configuration. */
    public record OddsApiProps(String baseUrl, String apiKey) {
        /** Applies defaults for null values. */
        public OddsApiProps {
            baseUrl = Objects.requireNonNullElse(baseUrl, ODDS_API_BASE_URL);
            apiKey = Objects.requireNonNullElse(apiKey, "");
        }
    }

    /** API-Sports configuration. */
    public record ApiSportsProps(String baseUrl, String apiKey) {
        /** Applies defaults for null values. */
        public ApiSportsProps {
            baseUrl = Objects.requireNonNullElse(baseUrl, API_SPORTS_BASE_URL);
            apiKey = Objects.requireNonNullElse(apiKey, "");
        }
    }

    /** ESPN API configuration. */
    public record EspnProps(String baseUrl) {
        /** Applies defaults for null values. */
        public EspnProps {
            baseUrl = Objects.requireNonNullElse(baseUrl, ESPN_BASE_URL);
        }
    }

    /** Open-Meteo API configuration. */
    public record OpenMeteoProps(String baseUrl) {
        /** Applies defaults for null values. */
        public OpenMeteoProps {
            baseUrl = Objects.requireNonNullElse(baseUrl, OPEN_METEO_BASE_URL);
        }
    }
}
