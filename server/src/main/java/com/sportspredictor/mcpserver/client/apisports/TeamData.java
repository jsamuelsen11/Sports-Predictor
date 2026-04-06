package com.sportspredictor.mcpserver.client.apisports;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Team data from API-Sports. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TeamData(TeamInfo team, Venue venue) {

    /** Team identity and metadata. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamInfo(int id, String name, String code, String country, int founded, String logo) {}

    /** Team venue information. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Venue(int id, String name, String city, int capacity, String surface) {}
}
