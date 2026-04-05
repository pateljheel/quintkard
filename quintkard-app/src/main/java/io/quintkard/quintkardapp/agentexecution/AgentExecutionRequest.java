package io.quintkard.quintkardapp.agentexecution;

import io.quintkard.quintkardapp.aimodel.AiToolScope;
import io.quintkard.quintkardapp.message.Message;

public record AgentExecutionRequest(
        Message message,
        AiToolScope toolScope
) {
}
