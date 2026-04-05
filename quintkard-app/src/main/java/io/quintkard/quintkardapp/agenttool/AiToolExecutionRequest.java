package io.quintkard.quintkardapp.agenttool;

import java.util.Map;

public record AiToolExecutionRequest(
        String userId,
        String conversationId,
        Map<String, Object> arguments
) {
}
