package com.sportspredictor.client.espn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** ESPN scoreboard API response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScoreboardResponse(List<Event> events) {

    /** A single sporting event on the scoreboard. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Event(String id, String date, String name, String shortName, List<Competition> competitions) {}

    /** A competition within an event (typically one per event). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Competition(String id, String date, List<Competitor> competitors, Status status) {}

    /** A team competing in the event. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Competitor(String id, Team team, String score, boolean winner) {}

    /** Team identity information. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Team(String id, String displayName, String abbreviation) {}

    /** Game status information. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(StatusType type) {}

    /** Status type with completion state. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatusType(String name, boolean completed) {}
}
