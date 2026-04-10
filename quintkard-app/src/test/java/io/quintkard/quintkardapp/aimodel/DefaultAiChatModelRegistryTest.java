package io.quintkard.quintkardapp.aimodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;

class DefaultAiChatModelRegistryTest {

    private AiModelCatalogProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AiModelCatalogProperties();
        properties.setModels(Map.of(
                "gemini-2.5-flash", new AiModelDefinition(AiProvider.GOOGLE_GENAI),
                "gpt-5.4-mini", new AiModelDefinition(AiProvider.OPENAI)
        ));
    }

    @Test
    void returnsGoogleModelForGoogleCatalogEntry() {
        GoogleGenAiChatModel googleModel = mock(GoogleGenAiChatModel.class);
        DefaultAiChatModelRegistry registry = new DefaultAiChatModelRegistry(
                properties,
                fixedProvider(googleModel),
                emptyProvider()
        );

        assertEquals(AiProvider.GOOGLE_GENAI, registry.providerFor("gemini-2.5-flash"));
        assertSame(googleModel, registry.get("gemini-2.5-flash"));
    }

    @Test
    void returnsOpenAiModelForOpenAiCatalogEntry() {
        OpenAiChatModel openAiChatModel = mock(OpenAiChatModel.class);
        DefaultAiChatModelRegistry registry = new DefaultAiChatModelRegistry(
                properties,
                emptyProvider(),
                fixedProvider(openAiChatModel)
        );

        assertEquals(AiProvider.OPENAI, registry.providerFor("gpt-5.4-mini"));
        assertSame(openAiChatModel, registry.get("gpt-5.4-mini"));
    }

    @Test
    void rejectsUnsupportedModel() {
        DefaultAiChatModelRegistry registry = new DefaultAiChatModelRegistry(
                properties,
                emptyProvider(),
                emptyProvider()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.providerFor("unknown-model")
        );

        assertEquals("Unsupported AI model: unknown-model", exception.getMessage());
    }

    @Test
    void rejectsNullOrBlankModelName() {
        DefaultAiChatModelRegistry registry = new DefaultAiChatModelRegistry(
                properties,
                emptyProvider(),
                emptyProvider()
        );

        IllegalArgumentException nullException = assertThrows(
                IllegalArgumentException.class,
                () -> registry.providerFor(null)
        );
        assertEquals("AI model is required", nullException.getMessage());

        IllegalArgumentException blankException = assertThrows(
                IllegalArgumentException.class,
                () -> registry.providerFor("   ")
        );
        assertEquals("AI model is required", blankException.getMessage());
    }

    @Test
    void rejectsConfiguredGoogleProviderWhenConcreteBeanMissing() {
        DefaultAiChatModelRegistry registry = new DefaultAiChatModelRegistry(
                properties,
                emptyProvider(),
                emptyProvider()
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> registry.get("gemini-2.5-flash")
        );

        assertEquals(
                "Google GenAI chat model is not configured for requested model gemini-2.5-flash",
                exception.getMessage()
        );
    }

    @Test
    void rejectsConfiguredProviderWhenConcreteBeanMissing() {
        DefaultAiChatModelRegistry registry = new DefaultAiChatModelRegistry(
                properties,
                emptyProvider(),
                emptyProvider()
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> registry.get("gpt-5.4-mini")
        );

        assertEquals(
                "OpenAI chat model is not configured for requested model gpt-5.4-mini",
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
            return getIfAvailable();
        }

        @Override
        public T getObject() {
            return value;
        }
    }
}
