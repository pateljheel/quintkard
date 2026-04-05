package io.quintkard.quintkardapp.aimodel;

public record AiMessage(
        AiMessageRole role,
        String content
) {
}
