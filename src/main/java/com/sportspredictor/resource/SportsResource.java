package com.sportspredictor.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportspredictor.service.ScheduleService;
import com.sportspredictor.service.SportLeagueMapping;
import com.sportspredictor.service.StatsService;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceTemplateSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers MCP resources for sports data: leagues, standings, schedules, and teams. */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SportsResource {

    private final SportLeagueMapping sportLeagueMapping;
    private final ScheduleService scheduleService;
    private final StatsService statsService;
    private final ObjectMapper objectMapper;

    /** Registers the static sports://leagues resource. */
    @Bean
    public List<SyncResourceSpecification> sportsResources() {
        return List.of(leaguesResource());
    }

    /** Registers URI template resources for sport-specific data. */
    @Bean
    public List<SyncResourceTemplateSpecification> sportsResourceTemplates() {
        return List.of(standingsTemplate(), todayScheduleTemplate(), weekScheduleTemplate());
    }

    private SyncResourceSpecification leaguesResource() {
        var resource = new McpSchema.Resource(
                "sports://leagues",
                "Supported Leagues",
                "All supported sports leagues with metadata",
                "application/json",
                null);
        return new SyncResourceSpecification(resource, (exchange, request) -> {
            var leagues = sportLeagueMapping.allLeagues();
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(request.uri(), "application/json", toJson(leagues))));
        });
    }

    private SyncResourceTemplateSpecification standingsTemplate() {
        var template = new McpSchema.ResourceTemplate(
                "sports://{sport}/standings",
                "League Standings",
                "Current league standings for a sport",
                "application/json",
                null);
        return new SyncResourceTemplateSpecification(template, (exchange, request) -> {
            String sport = extractSport(request.uri(), "standings");
            var result = statsService.getTeamRecord(sport, "");
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(request.uri(), "application/json", toJson(result))));
        });
    }

    private SyncResourceTemplateSpecification todayScheduleTemplate() {
        var template = new McpSchema.ResourceTemplate(
                "sports://{sport}/schedule/today",
                "Today's Schedule",
                "Today's games for a sport with current status",
                "application/json",
                null);
        return new SyncResourceTemplateSpecification(template, (exchange, request) -> {
            String sport = extractSport(request.uri(), "schedule");
            var result = scheduleService.getTodaySchedule(sport);
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(request.uri(), "application/json", toJson(result))));
        });
    }

    private SyncResourceTemplateSpecification weekScheduleTemplate() {
        var template = new McpSchema.ResourceTemplate(
                "sports://{sport}/schedule/week",
                "Weekly Schedule",
                "This week's full schedule for a sport",
                "application/json",
                null);
        return new SyncResourceTemplateSpecification(template, (exchange, request) -> {
            String sport = extractSport(request.uri(), "schedule");
            var result = scheduleService.getWeekSchedule(sport);
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(request.uri(), "application/json", toJson(result))));
        });
    }

    private String extractSport(String uri, String segment) {
        // URI format: sports://{sport}/... e.g., "sports://nfl/standings"
        String path = uri.replace("sports://", "");
        String[] parts = path.split("/");
        if (parts.length > 0) {
            return parts[0];
        }
        throw new IllegalArgumentException("Could not extract sport from URI: " + uri);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize resource to JSON: {}", e.getMessage());
            return "{}";
        }
    }
}
