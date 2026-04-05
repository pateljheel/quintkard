package io.quintkard.quintkardapp.agent;

import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Component;

@Component
public class AgentModelCatalog {

    private static final List<AgentModelConfigResponse> MODELS = List.of(
            new AgentModelConfigResponse("gpt-5.4-mini", "GPT-5.4 Mini", 0.0, 1.5, 0.7),
            new AgentModelConfigResponse("gpt-5.4", "GPT-5.4", 0.0, 1.5, 0.7),
            new AgentModelConfigResponse("gpt-5.2", "GPT-5.2", 0.0, 2.0, 0.7),
            new AgentModelConfigResponse("gpt-5.1-codex-mini", "GPT-5.1 Codex Mini", 0.0, 1.2, 0.4),
            new AgentModelConfigResponse("gemini-2.5-flash", "Gemini 2.5 Flash", 0.0, 2.0, 0.7)
    );

    public List<AgentModelConfigResponse> listModels() {
        return MODELS;
    }

    public AgentModelConfigResponse getModel(String modelId) {
        return MODELS.stream()
                .filter(model -> model.id().equals(modelId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Unsupported agent model: " + modelId));
    }
}
