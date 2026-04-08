package com.sportspredictor.agent.controller;

import com.sportspredictor.agent.workflow.DailyCycleWorkflow;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for manually triggering agent workflows. */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final DailyCycleWorkflow dailyCycleWorkflow;

    @PostMapping("/settle")
    public ResponseEntity<Map<String, String>> settle() {
        dailyCycleWorkflow.settle();
        return ResponseEntity.ok(Map.of("status", "completed", "workflow", "settle"));
    }

    @PostMapping("/scan-and-bet")
    public ResponseEntity<Map<String, String>> scanAndBet() {
        dailyCycleWorkflow.scanAndBet();
        return ResponseEntity.ok(Map.of("status", "completed", "workflow", "scan-and-bet"));
    }

    @PostMapping("/report")
    public ResponseEntity<Map<String, String>> report() {
        dailyCycleWorkflow.report();
        return ResponseEntity.ok(Map.of("status", "completed", "workflow", "report"));
    }

    @PostMapping("/full-cycle")
    public ResponseEntity<Map<String, String>> fullCycle() {
        dailyCycleWorkflow.runFullCycle();
        return ResponseEntity.ok(Map.of("status", "completed", "workflow", "full-cycle"));
    }
}
