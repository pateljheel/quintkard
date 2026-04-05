package io.quintkard.quintkardapp.message;

import java.time.Instant;
import java.util.UUID;

public record MessageListItemResponse(
        UUID id,
        String userId,
        String sourceService,
        String externalMessageId,
        String messageType,
        MessageStatus status,
        String summary,
        Instant ingestedAt,
        Instant sourceCreatedAt
) {

    public static MessageListItemResponse from(MessageSummaryProjection message) {
        return new MessageListItemResponse(
                message.getId(),
                message.getUserId(),
                message.getSourceService(),
                message.getExternalMessageId(),
                message.getMessageType(),
                message.getStatus(),
                message.getSummary(),
                message.getIngestedAt(),
                message.getSourceCreatedAt()
        );
    }
}
