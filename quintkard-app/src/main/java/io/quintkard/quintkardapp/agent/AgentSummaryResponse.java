package io.quintkard.quintkardapp.agent;

import java.util.UUID;

public record AgentSummaryResponse(
        UUID id,
        String userId,
        String name,
        String description,
        String model,
        double temperature
) {

    public static AgentSummaryResponse from(AgentConfig agent) {
        return new AgentSummaryResponse(
                agent.getId(),
                agent.getUser().getUserId(),
                agent.getName(),
                agent.getDescription(),
                agent.getModel(),
                agent.getTemperature()
        );
    }
}
