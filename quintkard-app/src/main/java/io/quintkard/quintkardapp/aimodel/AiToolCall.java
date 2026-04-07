package io.quintkard.quintkardapp.aimodel;

import java.util.Map;

public record AiToolCall(
        String toolCallId,
        String toolName,
        Map<String, Object> arguments
) {
}
