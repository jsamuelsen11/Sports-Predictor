package com.sportspredictor.mcpserver.config;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/** Creates observation spans for every MCP {@code @Tool} method invocation. */
@Aspect
@Component
@RequiredArgsConstructor
public class ToolObservationAspect {

    private final ObservationRegistry observationRegistry;

    /** Wraps MCP tool invocations with observation spans. */
    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object observeTool(ProceedingJoinPoint joinPoint) throws Throwable {
        String toolName = joinPoint.getSignature().getName();
        String toolClass = joinPoint.getTarget().getClass().getSimpleName();

        Observation observation = Observation.createNotStarted("mcp.tool.invocation", observationRegistry)
                .lowCardinalityKeyValue("mcp.tool.name", toolName)
                .lowCardinalityKeyValue("mcp.tool.class", toolClass);

        return observation.observe(() -> {
            try {
                return joinPoint.proceed();
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        });
    }
}
