package io.quintkard.quintkardapp.card;

import java.time.LocalDate;
import java.util.UUID;

public record CardRequest(
    String title,
    String summary,
    String content,
    CardType cardType,
    CardStatus status,
    CardPriority priority,
    LocalDate dueDate,
    UUID sourceMessageId
) {
}
