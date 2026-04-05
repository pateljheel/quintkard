package io.quintkard.quintkardapp.message;

import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageServiceImpl implements MessageService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final MessageRepository messageRepository;

    public MessageServiceImpl(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Message getMessage(String userId, UUID messageId) {
        return messageRepository.findByIdAndUserUserId(messageId, userId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<MessageSummaryProjection> listMessages(
            String userId,
            int page,
            int size,
            String query,
            MessageStatus status
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizePageSize(size));

        if (query == null || query.isBlank()) {
            if (status == null) {
                return messageRepository.findSummariesByUserUserIdOrderByIngestedAtDesc(userId, pageable);
            }
            return messageRepository.findSummariesByUserUserIdAndStatusOrderByIngestedAtDesc(userId, status, pageable);
        }

        return messageRepository.searchSummariesByUserId(
                userId,
                status == null ? null : status.name(),
                query.trim(),
                pageable
        );
    }

    @Override
    @Transactional
    public Message updateMessageStatus(String userId, UUID messageId, MessageStatus status) {
        Message message = getMessage(userId, messageId);
        applyStatus(message, status);
        return message;
    }

    @Override
    @Transactional
    public void deleteMessage(String userId, UUID messageId) {
        Message message = getMessage(userId, messageId);
        messageRepository.delete(message);
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private void applyStatus(Message message, MessageStatus status) {
        switch (status) {
            case PENDING -> message.markPending();
            case PROCESSING -> message.markProcessing();
            case FAILED -> message.markFailed();
            case SUCCESS -> message.markSuccess();
        }
    }
}
