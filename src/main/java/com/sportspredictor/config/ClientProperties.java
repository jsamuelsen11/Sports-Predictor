package com.sportspredictor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Externalized API client configuration, bound to {@code app.client} properties. */
@ConfigurationProperties(prefix = "app.client")
public record ClientProperties(
        OddsApiProps oddsApi, ApiSportsProps apiSports, EspnProps espn, OpenMeteoProps openMeteo) {

    /** Creates properties with default base URLs when values are not specified. */
    public ClientProperties {
        if (oddsApi == null) {
            oddsApi = new OddsApiProps("https://api.the-odds-api.com/v4", "");
        }
        if (apiSports == null) {
            apiSports = new ApiSportsProps("https://v3.football.api-sports.io", "");
        }
        if (espn == null) {
            espn = new EspnProps("https://site.api.espn.com");
        }
        if (openMeteo == null) {
            openMeteo = new OpenMeteoProps("https://api.open-meteo.com/v1");
        }
    }

    /** The Odds API configuration. */
    public record OddsApiProps(String baseUrl, String apiKey) {
        /** Applies defaults for null values. */
        public OddsApiProps {
            if (baseUrl == null) {
                baseUrl = "https://api.the-odds-api.com/v4";
            }
            if (apiKey == null) {
                apiKey = "";
            }
        }
    }

    /** API-Sports configuration. */
    public record ApiSportsProps(String baseUrl, String apiKey) {
        /** Applies defaults for null values. */
        public ApiSportsProps {
            if (baseUrl == null) {
                baseUrl = "https://v3.football.api-sports.io";
            }
            if (apiKey == null) {
                apiKey = "";
            }
        }
    }

    /** ESPN API configuration. */
    public record EspnProps(String baseUrl) {
        /** Applies defaults for null values. */
        public EspnProps {
            if (baseUrl == null) {
                baseUrl = "https://site.api.espn.com";
            }
        }
    }

    /** Open-Meteo API configuration. */
    public record OpenMeteoProps(String baseUrl) {
        /** Applies defaults for null values. */
        public OpenMeteoProps {
            if (baseUrl == null) {
                baseUrl = "https://api.open-meteo.com/v1";
            }
        }
    }
}
