package com.sportspredictor.client.oddsapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Historical odds response from The Odds API. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OddsHistoryResponse(
        String timestamp,
        @JsonProperty("previous_timestamp") String previousTimestamp,
        @JsonProperty("next_timestamp") String nextTimestamp,
        List<OddsResponse> data) {}
