package io.quintkard.quintkardapp.message;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Slice;

public interface MessageService {

    Message getMessage(String userId, UUID messageId);

    Slice<MessageSummaryProjection> listMessages(
            String userId,
            int page,
            int size,
            String query,
            MessageStatus status,
            String sourceService,
            String messageType,
            Instant ingestedAfter,
            Instant ingestedBefore
    );

    Message updateMessageStatus(String userId, UUID messageId, MessageStatus status);

    void deleteMessage(String userId, UUID messageId);
}
