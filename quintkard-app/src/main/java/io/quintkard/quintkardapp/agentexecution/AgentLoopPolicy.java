package io.quintkard.quintkardapp.agentexecution;

public record AgentLoopPolicy(
        int maxIterations,
        int maxToolCalls
) {
}
