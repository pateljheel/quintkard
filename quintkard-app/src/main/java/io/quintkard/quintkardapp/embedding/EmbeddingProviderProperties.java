package io.quintkard.quintkardapp.embedding;

import io.quintkard.quintkardapp.aimodel.AiProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quintkard.embedding.selection")
public class EmbeddingProviderProperties {

    private AiProvider provider = AiProvider.GOOGLE_GENAI;

    public AiProvider getProvider() {
        return provider;
    }

    public void setProvider(AiProvider provider) {
        this.provider = provider;
    }
}
