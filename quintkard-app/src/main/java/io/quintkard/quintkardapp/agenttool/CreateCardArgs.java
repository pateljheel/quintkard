package io.quintkard.quintkardapp.agenttool;

public record CreateCardArgs(
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
