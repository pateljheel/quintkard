package io.quintkard.quintkardapp.card;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CardListItemResponse(
        UUID id,
        String userId,
        String title,
        String summary,
        CardType cardType,
        CardStatus status,
        CardPriority priority,
        LocalDate dueDate,
        UUID sourceMessageId,
        Instant createdAt,
        Instant updatedAt
) {

    public static CardListItemResponse from(CardSummaryProjection card) {
        return new CardListItemResponse(
                card.getId(),
                card.getUserId(),
                card.getTitle(),
                card.getSummary(),
                card.getCardType(),
                card.getStatus(),
                card.getPriority(),
                card.getDueDate(),
                card.getSourceMessageId(),
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }
}
