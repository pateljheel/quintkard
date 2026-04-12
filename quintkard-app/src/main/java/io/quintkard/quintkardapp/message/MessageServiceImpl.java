package io.quintkard.quintkardapp.message;

import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public Slice<MessageSummaryProjection> listMessages(MessageFilter filter, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(Sort.Direction.DESC, "ingestedAt")
        );
        MessageFilter normalizedFilter = new MessageFilter(
                filter.userId(),
                trimToNull(filter.query()),
                filter.status(),
                trimToNull(filter.sourceService()),
                trimToNull(filter.messageType()),
                filter.ingestedAfter(),
                filter.ingestedBefore()
        );

        if (normalizedFilter.query() == null) {
            return messageRepository.findAll(MessageSpecifications.fromFilter(normalizedFilter), pageable)
                    .map(MessageSummaryItem::from);
        }

        return messageRepository.searchSummaries(
                normalizedFilter,
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
