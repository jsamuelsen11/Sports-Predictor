package com.sportspredictor.agent.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Orchestrates the agent's daily cycle via Spring's @Scheduled cron triggers.
 * Each phase is a separate ChatClient conversation where Claude autonomously
 * calls MCP tools and reasons about the results.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyCycleWorkflow {

    private final SettlementWorkflow settlementWorkflow;
    private final ScanWorkflow scanWorkflow;
    private final AnalyzeAndBetWorkflow analyzeAndBetWorkflow;
    private final ReportWorkflow reportWorkflow;

    @Scheduled(cron = "${agent.schedule.settle-cron}")
    public void settle() {
        log.info("=== DAILY CYCLE: Settlement Phase ===");
        try {
            String result = settlementWorkflow.execute();
            log.info("Settlement result:\n{}", result);
        } catch (Exception e) {
            log.error("Settlement workflow failed", e);
        }
    }

    @Scheduled(cron = "${agent.schedule.scan-cron}")
    public void scanAndBet() {
        log.info("=== DAILY CYCLE: Scan & Bet Phase ===");
        try {
            String candidates = scanWorkflow.execute();
            log.info("Scan candidates:\n{}", candidates);

            String betResult = analyzeAndBetWorkflow.execute(candidates);
            log.info("Bet placement result:\n{}", betResult);
        } catch (Exception e) {
            log.error("Scan and bet workflow failed", e);
        }
    }

    @Scheduled(cron = "${agent.schedule.report-cron}")
    public void report() {
        log.info("=== DAILY CYCLE: Report Phase ===");
        try {
            String report = reportWorkflow.execute();
            log.info("Daily report:\n{}", report);
        } catch (Exception e) {
            log.error("Report workflow failed", e);
        }
    }

    /** Manual trigger for running the full cycle on demand. */
    public void runFullCycle() {
        log.info("=== MANUAL: Running full daily cycle ===");
        settle();
        scanAndBet();
        report();
    }
}
