package io.quintkard.quintkardapp.agentexecution;

import io.quintkard.quintkardapp.agent.AgentConfig;
import io.quintkard.quintkardapp.messagepipeline.MessageProcessingContext;

public interface AgentExecutionService {

    AgentExecutionResult execute(
            MessageProcessingContext context,
            AgentConfig agentConfig,
            AgentExecutionRequest request
    );
}
