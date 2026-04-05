package io.quintkard.quintkardapp.orchestrator;

import java.util.List;
import java.util.UUID;

public record OrchestratorConfigRequest(
    String filteringPrompt,
    String filteringModel,
    String routingPrompt,
    String routingModel,
    List<UUID> activeAgentIds
) {
}
