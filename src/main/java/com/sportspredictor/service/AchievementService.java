package com.sportspredictor.service;

import com.sportspredictor.entity.Bet;
import com.sportspredictor.entity.enums.BetStatus;
import com.sportspredictor.entity.enums.BetType;
import com.sportspredictor.repository.BetRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gamification: achievement tracking based on bet history milestones. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AchievementService {

    private final BetRepository betRepository;
    private final BankrollService bankrollService;

    /** A single achievement with progress. */
    public record Achievement(String name, String description, boolean unlocked, double progressPercent) {}

    /** All achievements result. */
    public record AchievementsResult(
            List<Achievement> achievements, int unlockedCount, int totalCount, String summary) {}

    /** Computes all achievements based on bet history. */
    public AchievementsResult getAchievements() {
        var bankroll = bankrollService.getActiveBankroll();
        List<Bet> allBets = betRepository.findByBankrollId(bankroll.getId());
        List<Bet> settled = allBets.stream()
                .filter(b -> b.getStatus() == BetStatus.WON || b.getStatus() == BetStatus.LOST)
                .toList();

        int totalBets = allBets.size();
        long wins = settled.stream().filter(b -> b.getStatus() == BetStatus.WON).count();
        long parlayWins = settled.stream()
                .filter(b -> b.getBetType() == BetType.PARLAY && b.getStatus() == BetStatus.WON)
                .count();

        double totalProfit = settled.stream()
                .mapToDouble(b -> {
                    if (b.getStatus() == BetStatus.WON && b.getActualPayout() != null) {
                        return b.getActualPayout().subtract(b.getStake()).doubleValue();
                    }
                    if (b.getStatus() == BetStatus.LOST) {
                        return -b.getStake().doubleValue();
                    }
                    return 0.0;
                })
                .sum();

        int maxWinStreak = computeMaxWinStreak(settled);

        List<Achievement> achievements = new ArrayList<>();
        achievements.add(milestone("FIRST_BET", "Place your first bet", totalBets, 1));
        achievements.add(milestone("FIRST_WIN", "Win your first bet", (int) wins, 1));
        achievements.add(milestone("FIRST_PARLAY_WIN", "Win your first parlay", (int) parlayWins, 1));
        achievements.add(milestone("WIN_STREAK_5", "Achieve a 5-game win streak", maxWinStreak, 5));
        achievements.add(milestone("WIN_STREAK_10", "Achieve a 10-game win streak", maxWinStreak, 10));
        achievements.add(milestone("WIN_STREAK_25", "Achieve a 25-game win streak", maxWinStreak, 25));
        achievements.add(milestone("PROFIT_100", "Reach $100 in profit", (int) totalProfit, 100));
        achievements.add(milestone("PROFIT_500", "Reach $500 in profit", (int) totalProfit, 500));
        achievements.add(milestone("PROFIT_1000", "Reach $1,000 in profit", (int) totalProfit, 1000));
        achievements.add(milestone("BETS_50", "Place 50 bets", totalBets, 50));
        achievements.add(milestone("BETS_100", "Place 100 bets", totalBets, 100));
        achievements.add(milestone("BETS_500", "Place 500 bets", totalBets, 500));

        int unlocked = (int) achievements.stream().filter(Achievement::unlocked).count();

        return new AchievementsResult(
                achievements,
                unlocked,
                achievements.size(),
                String.format("%d/%d achievements unlocked.", unlocked, achievements.size()));
    }

    private static Achievement milestone(String name, String desc, int current, int target) {
        boolean unlocked = current >= target;
        double progress = Math.min(100.0, (double) current / target * 100.0);
        return new Achievement(name, desc, unlocked, progress);
    }

    private static int computeMaxWinStreak(List<Bet> settled) {
        int max = 0;
        int streak = 0;
        for (Bet b : settled) {
            if (b.getStatus() == BetStatus.WON) {
                streak++;
                if (streak > max) {
                    max = streak;
                }
            } else {
                streak = 0;
            }
        }
        return max;
    }
}
