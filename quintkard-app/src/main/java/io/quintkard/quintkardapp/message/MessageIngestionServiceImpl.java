package io.quintkard.quintkardapp.message;

import io.quintkard.quintkardapp.user.User;
import io.quintkard.quintkardapp.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class MessageIngestionServiceImpl implements MessageIngestionService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageIngestionServiceImpl(
        MessageRepository messageRepository,
        UserRepository userRepository
    ) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Message ingestMessage(String userId, MessageEnvelope envelope) {
        return messageRepository.save(buildMessage(userId, envelope));
    }

    @Override
    @Transactional
    public List<Message> ingestMessages(String userId, List<MessageEnvelope> envelopes) {
        if (envelopes == null || envelopes.isEmpty()) {
            return List.of();
        }

        List<Message> messages = new ArrayList<>();
        for (MessageEnvelope envelope : envelopes) {
            messages.add(buildMessage(userId, envelope));
        }

        return messageRepository.saveAll(messages);
    }

    private Message buildMessage(String userId, MessageEnvelope envelope) {
        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        String payload = redactPayload(user, envelope.payload());

        return new Message(
            user,
            envelope.sourceService(),
            envelope.externalMessageId(),
            envelope.messageType(),
            MessageStatus.PENDING,
            payload,
            buildSummary(payload),
            normalize(envelope.metadata()),
            normalize(envelope.details()),
            Instant.now(),
            envelope.sourceCreatedAt()
        );
    }

    private Map<String, Object> normalize(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value;
    }

    private String redactPayload(User user, String payload) {
        if (payload == null || payload.isBlank()) {
            return payload;
        }

        if (!user.isRedactionEnabled()) {
            return payload;
        }

        return payload;
    }

    private String buildSummary(String payload) {
        if (payload == null) {
            return null;
        }

        String normalized = payload.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return null;
        }

        int maxLength = 180;
        if (normalized.length() <= maxLength) {
            return normalized;
        }

        return normalized.substring(0, maxLength - 1).trim() + "...";
    }
}
