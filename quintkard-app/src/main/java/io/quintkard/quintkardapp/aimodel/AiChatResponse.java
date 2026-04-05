package io.quintkard.quintkardapp.aimodel;

import java.util.List;

public record AiChatResponse(
        String text,
        List<AiToolCall> toolCalls,
        boolean finalResponse
) {
}
