package io.quintkard.quintkardapp.orchestratorexecution;

import io.quintkard.quintkardapp.agentexecution.AgentExecutionResult;
import io.quintkard.quintkardapp.message.Message;
import io.quintkard.quintkardapp.messagepipeline.MessageProcessingContext;
import io.quintkard.quintkardapp.orchestrator.OrchestratorConfig;
import java.util.Map;
import java.util.UUID;

public interface AgentDispatchService {

    Map<UUID, AgentExecutionResult> dispatch(
            OrchestratorConfig config,
            RoutingDecision routingDecision,
            MessageProcessingContext context,
            Message message
    );
}
