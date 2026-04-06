package com.sportspredictor.mcpserver.client.oddsapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** A sport available on The Odds API. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SportResponse(
        String key,
        String group,
        String title,
        String description,
        boolean active,
        @JsonProperty("has_outrights") boolean hasOutrights) {}
