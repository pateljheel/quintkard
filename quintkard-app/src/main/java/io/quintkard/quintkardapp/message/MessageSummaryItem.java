package io.quintkard.quintkardapp.message;

import java.time.Instant;
import java.util.UUID;

public record MessageSummaryItem(
        UUID id,
        String userId,
        String sourceService,
        String externalMessageId,
        String messageType,
        MessageStatus status,
        String summary,
        Instant ingestedAt,
        Instant sourceCreatedAt
) implements MessageSummaryProjection {

    public static MessageSummaryItem from(Message message) {
        return new MessageSummaryItem(
                message.getId(),
                message.getUser().getUserId(),
                message.getSourceService(),
                message.getExternalMessageId(),
                message.getMessageType(),
                message.getStatus(),
                message.getSummary(),
                message.getIngestedAt(),
                message.getSourceCreatedAt()
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
    public String getSourceService() {
        return sourceService;
    }

    @Override
    public String getExternalMessageId() {
        return externalMessageId;
    }

    @Override
    public String getMessageType() {
        return messageType;
    }

    @Override
    public MessageStatus getStatus() {
        return status;
    }

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public Instant getIngestedAt() {
        return ingestedAt;
    }

    @Override
    public Instant getSourceCreatedAt() {
        return sourceCreatedAt;
    }
}
