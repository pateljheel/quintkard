package io.quintkard.quintkardapp.aimodel;

import org.springframework.ai.chat.prompt.ChatOptions;

public interface AiChatOptionsFactory {

    ChatOptions build(
            AiProvider provider,
            String userId,
            String model,
            double temperature,
            AiToolScope toolScope,
            String responseSchema,
            String responseMimeType
    );
}
