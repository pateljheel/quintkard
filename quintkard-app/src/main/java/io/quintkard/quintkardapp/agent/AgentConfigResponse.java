package io.quintkard.quintkardapp.agent;

import java.util.UUID;

public record AgentConfigResponse(
        UUID id,
        String userId,
        String name,
        String description,
        String prompt,
        String model,
        double temperature
) {

    public static AgentConfigResponse from(AgentConfig agent) {
        return new AgentConfigResponse(
                agent.getId(),
                agent.getUser().getUserId(),
                agent.getName(),
                agent.getDescription(),
                agent.getPrompt(),
                agent.getModel(),
                agent.getTemperature()
        );
    }
}
