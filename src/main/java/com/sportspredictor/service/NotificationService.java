package com.sportspredictor.service;

import com.sportspredictor.entity.Alert;
import com.sportspredictor.entity.Subscription;
import com.sportspredictor.entity.enums.BetStatus;
import com.sportspredictor.entity.enums.SubscriptionType;
import com.sportspredictor.repository.AlertRepository;
import com.sportspredictor.repository.BetRepository;
import com.sportspredictor.repository.SubscriptionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Notification system: daily briefings, subscriptions, and alerts. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final SubscriptionRepository subscriptionRepository;
    private final AlertRepository alertRepository;
    private final BetRepository betRepository;

    /** Daily briefing result. */
    public record BriefingResult(int pendingBetCount, int alertCount, String summary) {}

    /** Subscription result. */
    public record SubscribeResult(String subscriptionId, String type, String sport, String summary) {}

    /** Alert detail. */
    public record AlertDetail(String alertId, String type, String title, String message, String createdAt) {}

    /** Alerts result. */
    public record AlertsResult(List<AlertDetail> alerts, int count, String summary) {}

    /** Generates a daily briefing summary. */
    @Transactional(readOnly = true)
    public BriefingResult getDailyBriefing() {
        int pendingBets = betRepository.findByStatus(BetStatus.PENDING).size();
        int unreadAlerts = alertRepository.findByReadFalse().size();

        String summary = String.format("Daily briefing: %d pending bets, %d unread alerts.", pendingBets, unreadAlerts);

        return new BriefingResult(pendingBets, unreadAlerts, summary);
    }

    /** Creates a line movement subscription. */
    public SubscribeResult subscribeLineMovement(String sport, double threshold) {
        Subscription sub = Subscription.builder()
                .type(SubscriptionType.LINE_MOVEMENT)
                .sport(sport)
                .threshold(BigDecimal.valueOf(threshold))
                .active(true)
                .createdAt(Instant.now())
                .build();
        subscriptionRepository.save(sub);

        log.info("Line movement subscription created id={} sport={} threshold={}", sub.getId(), sport, threshold);

        return new SubscribeResult(
                sub.getId(),
                "LINE_MOVEMENT",
                sport,
                String.format("Subscribed to %s line movements > %.1f points.", sport, threshold));
    }

    /** Creates an injury update subscription. */
    public SubscribeResult subscribeInjuryUpdates(String sport) {
        Subscription sub = Subscription.builder()
                .type(SubscriptionType.INJURY_UPDATE)
                .sport(sport)
                .active(true)
                .createdAt(Instant.now())
                .build();
        subscriptionRepository.save(sub);

        log.info("Injury update subscription created id={} sport={}", sub.getId(), sport);

        return new SubscribeResult(
                sub.getId(), "INJURY_UPDATE", sport, String.format("Subscribed to %s injury updates.", sport));
    }

    /** Returns all unread alerts. */
    @Transactional(readOnly = true)
    public AlertsResult getAlerts() {
        List<Alert> unread = alertRepository.findByReadFalse();
        List<AlertDetail> details = unread.stream()
                .map(a -> new AlertDetail(
                        a.getId(),
                        a.getType(),
                        a.getTitle(),
                        a.getMessage(),
                        a.getCreatedAt().toString()))
                .toList();

        return new AlertsResult(details, details.size(), String.format("%d unread alerts.", details.size()));
    }
}
