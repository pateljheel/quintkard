package io.quintkard.quintkardapp.aimodel;

import java.util.List;

public record AiChatRequest(
        String userId,
        String model,
        double temperature,
        List<AiMessage> messages,
        AiMemoryScope memoryScope,
        AiToolScope toolScope
) {
}
