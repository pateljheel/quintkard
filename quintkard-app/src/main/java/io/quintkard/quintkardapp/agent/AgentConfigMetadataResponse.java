package io.quintkard.quintkardapp.agent;

import java.util.List;

public record AgentConfigMetadataResponse(
        List<AgentModelConfigResponse> models,
        String defaultAgentModelId,
        String defaultRoutingModelId,
        String defaultFilteringModelId
) {
}
