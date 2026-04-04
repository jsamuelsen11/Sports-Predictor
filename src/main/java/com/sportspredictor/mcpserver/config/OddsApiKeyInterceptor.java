package com.sportspredictor.mcpserver.config;

import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.util.UriComponentsBuilder;

/** Appends the {@code apiKey} query parameter to every Odds API request. */
public class OddsApiKeyInterceptor implements ClientHttpRequestInterceptor {

    private final String apiKey;

    /** Creates an interceptor that appends the given API key. */
    public OddsApiKeyInterceptor(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        if (apiKey.isEmpty()) {
            return execution.execute(request, body);
        }
        var uri = UriComponentsBuilder.fromUri(request.getURI())
                .queryParam("apiKey", apiKey)
                .build()
                .toUri();
        return execution.execute(
                new HttpRequestWrapper(request) {
                    @Override
                    public java.net.URI getURI() {
                        return uri;
                    }
                },
                body);
    }
}
