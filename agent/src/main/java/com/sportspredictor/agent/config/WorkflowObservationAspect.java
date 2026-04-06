package com.sportspredictor.agent.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
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

    @Around("execution(* com.sportspredictor.agent.workflow.*.execute(..))")
    public Object observeWorkflow(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String workflowName = className.replace("Workflow", "").toLowerCase();

        Observation observation = Observation.createNotStarted("agent.workflow", observationRegistry)
                .lowCardinalityKeyValue("workflow.name", workflowName);

        return observation.observe(() -> {
            try {
                Object result = joinPoint.proceed();
                Counter.builder("agent.workflow.runs")
                        .tag("workflow", workflowName)
                        .tag("outcome", "success")
                        .register(meterRegistry)
                        .increment();
                return result;
            } catch (RuntimeException | Error e) {
                Counter.builder("agent.workflow.runs")
                        .tag("workflow", workflowName)
                        .tag("outcome", "failure")
                        .register(meterRegistry)
                        .increment();
                throw e;
            } catch (Throwable t) {
                Counter.builder("agent.workflow.runs")
                        .tag("workflow", workflowName)
                        .tag("outcome", "failure")
                        .register(meterRegistry)
                        .increment();
                throw new RuntimeException(t);
            }
        });
    }
}
