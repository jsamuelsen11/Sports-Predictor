package com.sportspredictor.mcpserver.client.espn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** ESPN roster API response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RosterResponse(List<AthleteGroup> athletes) {

    /** Athletes grouped by position category. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AthleteGroup(String position, List<Athlete> items) {}

    /** An individual athlete on the roster. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Athlete(String id, String displayName, String jersey, String position, int age, String weight) {}
}
