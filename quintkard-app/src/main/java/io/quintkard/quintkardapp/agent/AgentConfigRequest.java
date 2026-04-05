package io.quintkard.quintkardapp.agent;

public record AgentConfigRequest(
        String name,
        String description,
        String prompt,
        String model,
        double temperature
) {
}