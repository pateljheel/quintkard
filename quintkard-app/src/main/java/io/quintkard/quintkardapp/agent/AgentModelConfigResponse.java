package io.quintkard.quintkardapp.agent;

public record AgentModelConfigResponse(
        String id,
        String label,
        double minTemperature,
        double maxTemperature,
        double defaultTemperature
) {
}
