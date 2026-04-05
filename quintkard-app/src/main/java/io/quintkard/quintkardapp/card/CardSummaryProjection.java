package io.quintkard.quintkardapp.card;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public interface CardSummaryProjection {

    UUID getId();

    String getUserId();

    String getTitle();

    String getSummary();

    CardType getCardType();

    CardStatus getStatus();

    CardPriority getPriority();

    LocalDate getDueDate();

    UUID getSourceMessageId();

    Instant getCreatedAt();

    Instant getUpdatedAt();
}
