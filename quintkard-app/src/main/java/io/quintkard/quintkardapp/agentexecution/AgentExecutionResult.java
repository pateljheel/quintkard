package io.quintkard.quintkardapp.agentexecution;

import io.quintkard.quintkardapp.aimodel.AiToolResult;
import java.util.List;

public record AgentExecutionResult(
        String responseText,
        AgentExecutionStatus status,
        int iterations,
        int toolCalls,
        List<AiToolResult> toolResults
) {
}
