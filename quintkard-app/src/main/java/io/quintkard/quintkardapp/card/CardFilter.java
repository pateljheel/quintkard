package io.quintkard.quintkardapp.card;

import java.time.Instant;

public record CardFilter(
        String userId,
        String query,
        CardStatus status,
        CardType cardType,
        Instant updatedAfter,
        Instant updatedBefore
) {
}
