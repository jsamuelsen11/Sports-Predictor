package com.sportspredictor.mcpserver.client.espn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** ESPN injuries API response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InjuryResponse(List<TeamInjuries> injuries) {

    /** Injuries grouped by team. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamInjuries(Team team, List<Injury> injuries) {}

    /** Team identity. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Team(String id, String displayName, String abbreviation) {}

    /** An individual injury entry. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Injury(Athlete athlete, String status, String date, String description) {}

    /** Injured athlete identity. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Athlete(String id, String displayName, String position) {}
}
