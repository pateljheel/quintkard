package io.quintkard.quintkardapp.orchestratorexecution;

import io.quintkard.quintkardapp.agentexecution.AgentExecutionResult;
import java.util.Map;
import java.util.UUID;

public record OrchestratorExecutionResult(
        FilteringDecision filteringDecision,
        RoutingDecision routingDecision,
        Map<UUID, AgentExecutionResult> agentResults
) {
}
