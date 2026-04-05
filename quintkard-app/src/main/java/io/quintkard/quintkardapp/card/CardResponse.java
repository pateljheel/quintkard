package io.quintkard.quintkardapp.card;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CardResponse(
    UUID id,
    String userId,
    String title,
    String summary,
    String content,
    CardType cardType,
    CardStatus status,
    CardPriority priority,
    LocalDate dueDate,
    UUID sourceMessageId,
    Instant createdAt,
    Instant updatedAt
) {

    public static CardResponse from(Card card) {
        return new CardResponse(
            card.getId(),
            card.getUser().getUserId(),
            card.getTitle(),
            card.getSummary(),
            card.getContent(),
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
