package io.quintkard.quintkardapp.aimodel;

public record AiToolResult(
        String toolCallId,
        String toolName,
        Object result
) {
}
