package io.quintkard.quintkardapp.aimodel;

public record AiModelDefinition(
        String id,
        String label,
        AiProvider provider,
        double minTemperature,
        double maxTemperature,
        double defaultTemperature,
        boolean defaultForAgent,
        boolean defaultForRouting,
        boolean defaultForFiltering
) {
}
