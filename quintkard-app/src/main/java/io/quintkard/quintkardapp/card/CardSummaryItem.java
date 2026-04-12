package io.quintkard.quintkardapp.card;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CardSummaryItem(
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
) implements CardSummaryProjection {

    public static CardSummaryItem from(Card card) {
        return new CardSummaryItem(
                card.getId(),
                card.getUser().getUserId(),
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

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public CardType getCardType() {
        return cardType;
    }

    @Override
    public CardStatus getStatus() {
        return status;
    }

    @Override
    public CardPriority getPriority() {
        return priority;
    }

    @Override
    public LocalDate getDueDate() {
        return dueDate;
    }

    @Override
    public UUID getSourceMessageId() {
        return sourceMessageId;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
