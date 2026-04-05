package io.quintkard.quintkardapp.orchestrator;

import io.quintkard.quintkardapp.agent.AgentConfigResponse;

import java.util.List;
import java.util.UUID;

public record OrchestratorConfigResponse(
    UUID id,
    String userId,
    String filteringPrompt,
    String filteringModel,
    String routingPrompt,
    String routingModel,
    List<AgentConfigResponse> activeAgents
) {

    public static OrchestratorConfigResponse from(OrchestratorConfig config) {
        return new OrchestratorConfigResponse(
            config.getId(),
            config.getUser().getUserId(),
            config.getFilteringPrompt(),
            config.getFilteringModel(),
            config.getRoutingPrompt(),
            config.getRoutingModel(),
            config.getActiveAgents().stream()
                .map(AgentConfigResponse::from)
                .toList()
        );
    }
}
