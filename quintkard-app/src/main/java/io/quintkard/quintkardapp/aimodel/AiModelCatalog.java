package io.quintkard.quintkardapp.aimodel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class AiModelCatalog {

    private static final List<AiModelDefinition> MODELS = List.of(
            new AiModelDefinition("gemini-2.5-flash", "Gemini 2.5 Flash", AiProvider.GOOGLE_GENAI, 0.0, 2.0, 0.7,
                    true, true, true),
            new AiModelDefinition("gpt-5.4-mini", "GPT-5.4 Mini", AiProvider.OPENAI, 0.0, 1.5, 0.7,
                    false, false, false),
            new AiModelDefinition("gpt-5.4", "GPT-5.4", AiProvider.OPENAI, 0.0, 1.5, 0.7,
                    false, false, false),
            new AiModelDefinition("gpt-5.2", "GPT-5.2", AiProvider.OPENAI, 0.0, 2.0, 0.7,
                    false, false, false),
            new AiModelDefinition("gpt-5.1-codex-mini", "GPT-5.1 Codex Mini", AiProvider.OPENAI, 0.0, 1.2, 0.4,
                    false, false, false)
    );

    private final Map<String, AiModelDefinition> modelsById;

    public AiModelCatalog() {
        LinkedHashMap<String, AiModelDefinition> models = new LinkedHashMap<>();
        for (AiModelDefinition model : MODELS) {
            models.put(model.id(), model);
        }
        this.modelsById = Map.copyOf(models);
    }

    public List<AiModelDefinition> listModels() {
        return MODELS;
    }

    public AiModelDefinition getModel(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            throw new IllegalArgumentException("AI model is required");
        }

        AiModelDefinition model = modelsById.get(modelId.trim());
        if (model == null) {
            throw new NoSuchElementException("Unsupported AI model: " + modelId);
        }
        return model;
    }

    public AiModelDefinition defaultAgentModel() {
        return findDefault(AiModelDefinition::defaultForAgent, "agent");
    }

    public AiModelDefinition defaultRoutingModel() {
        return findDefault(AiModelDefinition::defaultForRouting, "routing");
    }

    public AiModelDefinition defaultFilteringModel() {
        return findDefault(AiModelDefinition::defaultForFiltering, "filtering");
    }

    private AiModelDefinition findDefault(ModelSelector selector, String usage) {
        return MODELS.stream()
                .filter(selector::matches)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No default AI model configured for " + usage));
    }

    @FunctionalInterface
    private interface ModelSelector {
        boolean matches(AiModelDefinition model);
    }
}
