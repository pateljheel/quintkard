package io.quintkard.quintkardapp.aimodel;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class DefaultAiChatModelRegistry implements AiChatModelRegistry {

    private final AiModelCatalog modelCatalog;
    private final ObjectProvider<GoogleGenAiChatModel> googleChatModelProvider;
    private final ObjectProvider<OpenAiChatModel> openAiChatModelProvider;

    public DefaultAiChatModelRegistry(
            AiModelCatalog modelCatalog,
            ObjectProvider<GoogleGenAiChatModel> googleChatModelProvider,
            ObjectProvider<OpenAiChatModel> openAiChatModelProvider
    ) {
        this.modelCatalog = modelCatalog;
        this.googleChatModelProvider = googleChatModelProvider;
        this.openAiChatModelProvider = openAiChatModelProvider;
    }

    @Override
    public ChatModel get(String modelName) {
        return switch (providerFor(modelName)) {
            case GOOGLE_GENAI -> requireConfiguredModel(
                    modelName,
                    "Google GenAI",
                    googleChatModelProvider.getIfAvailable()
            );
            case OPENAI -> requireConfiguredModel(
                    modelName,
                    "OpenAI",
                    openAiChatModelProvider.getIfAvailable()
            );
        };
    }

    @Override
    public AiProvider providerFor(String modelName) {
        return modelCatalog.getModel(modelName).provider();
    }

    private ChatModel requireConfiguredModel(String modelName, String providerName, ChatModel chatModel) {
        if (chatModel == null) {
            throw new IllegalStateException(
                    "%s chat model is not configured for requested model %s".formatted(providerName, modelName)
            );
        }
        return chatModel;
    }
}
