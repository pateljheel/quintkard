package io.quintkard.quintkardapp.message;

import java.util.UUID;
import org.springframework.data.domain.Slice;

public interface MessageService {

    Message getMessage(String userId, UUID messageId);

    Slice<MessageSummaryProjection> listMessages(MessageFilter filter, int page, int size);

    Message updateMessageStatus(String userId, UUID messageId, MessageStatus status);

    void deleteMessage(String userId, UUID messageId);
}
