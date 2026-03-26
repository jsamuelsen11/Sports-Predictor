package com.sportspredictor.tool;

import com.sportspredictor.service.AchievementService;
import com.sportspredictor.service.AchievementService.AchievementsResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/** MCP tool for gamification achievement tracking. */
@Service
@RequiredArgsConstructor
public class AchievementTool {

    private final AchievementService achievementService;

    /** Response record. */
    public record AchievementsResponse(int unlockedCount, int totalCount, String summary) {}

    /** Gets all achievements with progress. */
    @Tool(
            name = "get_achievements",
            description = "Track milestones: win streaks, profit marks, bet counts, parlay wins")
    public AchievementsResponse getAchievements() {
        AchievementsResult r = achievementService.getAchievements();
        return new AchievementsResponse(r.unlockedCount(), r.totalCount(), r.summary());
    }
}
