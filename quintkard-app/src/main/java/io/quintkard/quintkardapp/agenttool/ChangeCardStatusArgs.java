package io.quintkard.quintkardapp.agenttool;

public record ChangeCardStatusArgs(
        String cardId,
        String status
) {
}
