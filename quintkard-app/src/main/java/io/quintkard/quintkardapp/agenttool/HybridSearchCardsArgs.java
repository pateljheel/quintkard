package io.quintkard.quintkardapp.agenttool;

public record HybridSearchCardsArgs(
        String query,
        String status,
        Integer limit
) {
}
