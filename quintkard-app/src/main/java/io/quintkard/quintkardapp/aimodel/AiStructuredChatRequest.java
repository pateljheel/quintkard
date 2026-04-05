package io.quintkard.quintkardapp.aimodel;

import java.util.List;

public record AiStructuredChatRequest<T>(
        String userId,
        String model,
        double temperature,
        List<AiMessage> messages,
        AiMemoryScope memoryScope,
        AiToolScope toolScope,
        Class<T> responseType
) {
}
