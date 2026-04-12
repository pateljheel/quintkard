package io.quintkard.quintkardapp.message;

import java.time.Instant;

public record MessageFilter(
        String userId,
        String query,
        MessageStatus status,
        String sourceService,
        String messageType,
        Instant ingestedAfter,
        Instant ingestedBefore
) {
}
