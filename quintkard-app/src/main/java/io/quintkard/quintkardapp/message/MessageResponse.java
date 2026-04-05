package io.quintkard.quintkardapp.message;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MessageResponse(
    UUID id,
    String userId,
    String sourceService,
    String externalMessageId,
    String messageType,
    MessageStatus status,
    String summary,
    String payload,
    Map<String, Object> metadata,
    Map<String, Object> details,
    Instant ingestedAt,
    Instant sourceCreatedAt
) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(
            message.getId(),
            message.getUser().getUserId(),
            message.getSourceService(),
            message.getExternalMessageId(),
            message.getMessageType(),
            message.getStatus(),
            message.getSummary(),
            message.getPayload(),
            message.getMetadata(),
            message.getDetails(),
            message.getIngestedAt(),
            message.getSourceCreatedAt()
        );
    }
}
