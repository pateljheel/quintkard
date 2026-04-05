package io.quintkard.quintkardapp.orchestrator;

public interface OrchestratorService {

    OrchestratorConfig getConfig(String userId);

    OrchestratorConfig updateConfig(String userId, OrchestratorConfigRequest request);
}
