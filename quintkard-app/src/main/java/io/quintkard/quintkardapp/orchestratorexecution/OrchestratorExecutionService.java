package io.quintkard.quintkardapp.orchestratorexecution;

import io.quintkard.quintkardapp.message.Message;
import io.quintkard.quintkardapp.orchestrator.OrchestratorConfig;

public interface OrchestratorExecutionService {

    OrchestratorExecutionResult execute(OrchestratorConfig config, Message message);
}
