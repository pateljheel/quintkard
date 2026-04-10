package io.quintkard.quintkardapp.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableConfigurationProperties({
        EmbeddingProperties.class,
        EmbeddingProviderProperties.class
})
public class EmbeddingConfiguration {

    @Bean
    @Primary
    EmbeddingModel embeddingModel(
            EmbeddingProviderProperties properties,
            @Qualifier("googleGenAiTextEmbedding") ObjectProvider<EmbeddingModel> googleEmbeddingProvider,
            @Qualifier("openAiEmbeddingModel") ObjectProvider<EmbeddingModel> openAiEmbeddingProvider
    ) {
        return switch (properties.getProvider()) {
            case GOOGLE_GENAI -> requireConfiguredEmbeddingModel(
                    "Google GenAI",
                    googleEmbeddingProvider.getIfAvailable()
            );
            case OPENAI -> requireConfiguredEmbeddingModel(
                    "OpenAI",
                    openAiEmbeddingProvider.getIfAvailable()
            );
        };
    }

    private EmbeddingModel requireConfiguredEmbeddingModel(String providerName, EmbeddingModel embeddingModel) {
        if (embeddingModel == null) {
            throw new IllegalStateException(providerName + " embedding model is not configured");
        }
        return embeddingModel;
    }
}
