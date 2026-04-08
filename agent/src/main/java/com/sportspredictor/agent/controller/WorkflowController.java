package com.sportspredictor.agent.controller;

import com.sportspredictor.agent.workflow.DailyCycleWorkflow;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for manually triggering agent workflows. */
@Slf4j
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final DailyCycleWorkflow dailyCycleWorkflow;
    private final ConcurrentHashMap<String, String> workflowStatus = new ConcurrentHashMap<>();

    @PostMapping("/settle")
    public ResponseEntity<Map<String, String>> settle() {
        return submitAsync("settle", dailyCycleWorkflow::settle);
    }

    @PostMapping("/scan-and-bet")
    public ResponseEntity<Map<String, String>> scanAndBet() {
        return submitAsync("scan-and-bet", dailyCycleWorkflow::scanAndBet);
    }

    @PostMapping("/report")
    public ResponseEntity<Map<String, String>> report() {
        return submitAsync("report", dailyCycleWorkflow::report);
    }

    @PostMapping("/full-cycle")
    public ResponseEntity<Map<String, String>> fullCycle() {
        return submitAsync("full-cycle", dailyCycleWorkflow::runFullCycle);
    }

    @GetMapping("/status/{correlationId}")
    public ResponseEntity<Map<String, String>> status(@PathVariable String correlationId) {
        String state = workflowStatus.get(correlationId);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("correlationId", correlationId, "status", state));
    }

    private ResponseEntity<Map<String, String>> submitAsync(String workflow, Runnable task) {
        String correlationId = UUID.randomUUID().toString();
        workflowStatus.put(correlationId, "running");

        CompletableFuture.runAsync(task).whenComplete((result, error) -> {
            if (error != null) {
                log.error("Workflow '{}' failed (correlationId={})", workflow, correlationId, error);
                workflowStatus.put(correlationId, "failed");
            } else {
                log.info("Workflow '{}' completed (correlationId={})", workflow, correlationId);
                workflowStatus.put(correlationId, "completed");
            }
        });

        return ResponseEntity.accepted()
                .body(Map.of("status", "accepted", "workflow", workflow, "correlationId", correlationId));
    }
}
