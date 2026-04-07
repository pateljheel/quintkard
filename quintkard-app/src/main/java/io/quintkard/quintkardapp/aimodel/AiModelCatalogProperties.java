package io.quintkard.quintkardapp.aimodel;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quintkard.ai")
public class AiModelCatalogProperties {

    private Map<String, AiModelDefinition> models = new LinkedHashMap<>();

    public Map<String, AiModelDefinition> getModels() {
        return models;
    }

    public void setModels(Map<String, AiModelDefinition> models) {
        this.models = models;
    }
}
