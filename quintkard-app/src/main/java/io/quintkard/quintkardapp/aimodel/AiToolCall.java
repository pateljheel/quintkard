package io.quintkard.quintkardapp.aimodel;

import java.util.Map;

public record AiToolCall(
        String toolName,
        Map<String, Object> arguments
) {
}
