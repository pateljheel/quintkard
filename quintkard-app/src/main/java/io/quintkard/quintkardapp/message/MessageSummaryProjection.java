package io.quintkard.quintkardapp.message;

import java.time.Instant;
import java.util.UUID;

public interface MessageSummaryProjection {

    UUID getId();

    String getUserId();

    String getSourceService();

    String getExternalMessageId();

    String getMessageType();

    MessageStatus getStatus();

    String getSummary();

    Instant getIngestedAt();

    Instant getSourceCreatedAt();
}
