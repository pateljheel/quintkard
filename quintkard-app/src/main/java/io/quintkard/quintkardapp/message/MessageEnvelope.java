package io.quintkard.quintkardapp.message;

import java.time.Instant;
import java.util.Map;

public record MessageEnvelope(
    String sourceService,
    String externalMessageId,
    String messageType,
    String payload,
    Map<String, Object> metadata,
    Map<String, Object> details,
    Instant sourceCreatedAt
) {
}
