package com.sportspredictor.client.apisports;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Generic API-Sports response wrapper — all endpoints share this structure. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiSportsResponse<T>(int results, Paging paging, List<T> response) {

    /** Pagination metadata. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Paging(int current, int total) {}
}
