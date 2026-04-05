package io.quintkard.quintkardapp.agenttool;

public record UpdateCardArgs(
        String cardId,
        String title,
        String summary,
        String content,
        String cardType,
        String status,
        String priority,
        String dueDate,
        String sourceMessageId
) {
}
