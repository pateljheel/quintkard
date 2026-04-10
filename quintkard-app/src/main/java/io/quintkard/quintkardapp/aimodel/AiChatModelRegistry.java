package io.quintkard.quintkardapp.aimodel;

import org.springframework.ai.chat.model.ChatModel;

public interface AiChatModelRegistry {

    ChatModel get(String modelName);

    AiProvider providerFor(String modelName);
}
