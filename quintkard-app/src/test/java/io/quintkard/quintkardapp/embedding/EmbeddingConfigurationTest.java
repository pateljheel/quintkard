package io.quintkard.quintkardapp.embedding;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import io.quintkard.quintkardapp.aimodel.AiProvider;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;

class EmbeddingConfigurationTest {

    private final EmbeddingConfiguration configuration = new EmbeddingConfiguration();

    @Test
    void selectsGoogleEmbeddingModelWhenConfigured() {
        EmbeddingProviderProperties properties = new EmbeddingProviderProperties();
        properties.setProvider(AiProvider.GOOGLE_GENAI);
        EmbeddingModel googleEmbeddingModel = mock(EmbeddingModel.class);

        EmbeddingModel selected = configuration.embeddingModel(
                properties,
                fixedProvider(googleEmbeddingModel),
                emptyProvider()
        );

        assertSame(googleEmbeddingModel, selected);
    }

    @Test
    void selectsOpenAiEmbeddingModelWhenConfigured() {
        EmbeddingProviderProperties properties = new EmbeddingProviderProperties();
        properties.setProvider(AiProvider.OPENAI);
        EmbeddingModel openAiEmbeddingModel = mock(EmbeddingModel.class);

        EmbeddingModel selected = configuration.embeddingModel(
                properties,
                emptyProvider(),
                fixedProvider(openAiEmbeddingModel)
        );

        assertSame(openAiEmbeddingModel, selected);
    }

    @Test
    void failsWhenConfiguredEmbeddingProviderIsMissing() {
        EmbeddingProviderProperties properties = new EmbeddingProviderProperties();
        properties.setProvider(AiProvider.GOOGLE_GENAI);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> configuration.embeddingModel(properties, emptyProvider(), emptyProvider())
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                "Google GenAI embedding model is not configured",
                exception.getMessage()
        );
    }

    private <T> ObjectProvider<T> fixedProvider(T value) {
        return new SimpleObjectProvider<>(value);
    }

    private <T> ObjectProvider<T> emptyProvider() {
        return new SimpleObjectProvider<>(null);
    }

    private static final class SimpleObjectProvider<T> implements ObjectProvider<T> {

        private final T value;

        private SimpleObjectProvider(T value) {
            this.value = value;
        }

        @Override
        public T getObject(Object... args) {
            return value;
        }

        @Override
        public T getIfAvailable() {
            return value;
        }

        @Override
        public T getIfUnique() {
            return value;
        }

        @Override
        public T getObject() {
            return value;
        }
    }
}
