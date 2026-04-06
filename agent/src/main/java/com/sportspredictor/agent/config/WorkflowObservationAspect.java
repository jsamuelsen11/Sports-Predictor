package com.sportspredictor.agent.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/** Creates observation spans and counters for agent workflow executions. */
@Aspect
@Component
@RequiredArgsConstructor
public class WorkflowObservationAspect {

    private final ObservationRegistry observationRegistry;
    private final MeterRegistry meterRegistry;

    /** Wraps workflow executions with observation spans and outcome counters. */
    @Around("execution(* com.sportspredictor.agent.workflow.*.execute(..))")
    public Object observeWorkflow(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String workflowName = className.replace("Workflow", "").toLowerCase(Locale.ROOT);

        Observation observation = Observation.createNotStarted("agent.workflow", observationRegistry)
                .lowCardinalityKeyValue("workflow.name", workflowName);

        observation.start();
        try {
            Object result = joinPoint.proceed();
            meterRegistry.counter("agent.workflow.runs", "workflow", workflowName, "outcome", "success")
                    .increment();
            return result;
        } catch (Throwable t) {
            observation.error(t);
            meterRegistry.counter("agent.workflow.runs", "workflow", workflowName, "outcome", "failure")
                    .increment();
            throw t;
        } finally {
            observation.stop();
        }
    }
}
