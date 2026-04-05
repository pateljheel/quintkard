package io.quintkard.quintkardapp.agentexecution;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quintkard.agent.execution")
public record AgentExecutionProperties(
        int maxIterations,
        int maxToolCalls
) {

    public AgentExecutionProperties {
        maxIterations = maxIterations <= 0 ? 30 : maxIterations;
        maxToolCalls = maxToolCalls <= 0 ? 100 : maxToolCalls;
    }
}
