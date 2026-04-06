package com.sportspredictor.mcpserver.config;

import com.sportspredictor.mcpserver.client.apisports.ApiSportsClient;
import com.sportspredictor.mcpserver.client.espn.EspnApiClient;
import com.sportspredictor.mcpserver.client.oddsapi.OddsApiClient;
import com.sportspredictor.mcpserver.client.openmeteo.OpenMeteoClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/** Creates RestClient-backed {@code @HttpExchange} proxy beans for all external API clients. */
@Configuration
@EnableConfigurationProperties(ClientProperties.class)
public class RestClientConfig {

    /** Open-Meteo weather API client — no authentication required. */
    @Bean
    public OpenMeteoClient openMeteoClient(ClientProperties props) {
        RestClient restClient =
                RestClient.builder().baseUrl(props.openMeteo().baseUrl()).build();
        return createProxy(restClient, OpenMeteoClient.class);
    }

    /** ESPN hidden API client — no authentication required. */
    @Bean
    public EspnApiClient espnApiClient(ClientProperties props) {
        RestClient restClient =
                RestClient.builder().baseUrl(props.espn().baseUrl()).build();
        return createProxy(restClient, EspnApiClient.class);
    }

    /** The Odds API client — API key injected as {@code apiKey} query parameter. */
    @Bean
    public OddsApiClient oddsApiClient(ClientProperties props) {
        RestClient restClient = RestClient.builder()
                .baseUrl(props.oddsApi().baseUrl())
                .requestInterceptor(new OddsApiKeyInterceptor(props.oddsApi().apiKey()))
                .build();
        return createProxy(restClient, OddsApiClient.class);
    }

    /** API-Sports client — API key injected as {@code x-apisports-key} header. */
    @Bean
    public ApiSportsClient apiSportsClient(ClientProperties props) {
        String apiKey = props.apiSports().apiKey();
        RestClient.Builder builder =
                RestClient.builder().baseUrl(props.apiSports().baseUrl());
        if (!apiKey.isEmpty()) {
            builder.defaultHeader("x-apisports-key", apiKey);
        }
        return createProxy(builder.build(), ApiSportsClient.class);
    }

    private <T> T createProxy(RestClient restClient, Class<T> clientClass) {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(clientClass);
    }
}
