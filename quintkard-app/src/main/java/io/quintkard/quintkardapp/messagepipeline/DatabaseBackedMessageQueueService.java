package io.quintkard.quintkardapp.messagepipeline;

import io.quintkard.quintkardapp.config.MessageQueueProperties;
import io.quintkard.quintkardapp.logging.LogContext;
import io.quintkard.quintkardapp.message.Message;
import io.quintkard.quintkardapp.message.MessageRepository;
import io.quintkard.quintkardapp.message.MessageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@ConditionalOnProperty(
    value = "quintkard.message.queue.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class DatabaseBackedMessageQueueService implements MessageQueueService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackedMessageQueueService.class);

    private final int batchSize;
    private final MessageProcessor messageProcessor;
    private final MessageRepository messageRepository;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final TransactionTemplate transactionTemplate;
    private final AtomicBoolean workerRunning = new AtomicBoolean(false);

    public DatabaseBackedMessageQueueService(
        MessageRepository messageRepository,
        MessageProcessor messageProcessor,
        PlatformTransactionManager transactionManager,
        @Qualifier("messageQueueTaskExecutor") ThreadPoolTaskExecutor taskExecutor,
        MessageQueueProperties messageQueueProperties
    ) {
        this.batchSize = messageQueueProperties.getBatchSize();
        this.messageProcessor = messageProcessor;
        this.messageRepository = messageRepository;
        this.taskExecutor = taskExecutor;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    @Scheduled(fixedDelayString = "${quintkard.message.queue.poll-delay-ms:5000}")
    public void triggerPendingMessageProcessing() {
        if (!workerRunning.compareAndSet(false, true)) {
            return;
        }

        taskExecutor.execute(this::drainPendingMessages);
    }

    private void drainPendingMessages() {
        try {
            while (processNextBatch() > 0) {
                // Keep draining until there are no more pending messages.
            }
        } finally {
            workerRunning.set(false);
            if (hasPendingMessages()) {
                triggerPendingMessageProcessing();
            }
        }
    }

    private int processNextBatch() {
        List<Message> messages = claimPendingMessages();
        for (Message message : messages) {
            processMessage(message);
        }
        return messages.size();
    }

    private boolean hasPendingMessages() {
        return transactionTemplate.execute(
            status -> messageRepository.existsByStatus(MessageStatus.PENDING)
        );
    }

    private List<Message> claimPendingMessages() {
        return transactionTemplate.execute(status -> {
            List<UUID> ids = messageRepository.claimMessageIdsByStatus(MessageStatus.PENDING.name(), batchSize);
            if (ids == null || ids.isEmpty()) {
                return List.of();
            }

            List<Message> fetchedMessages = messageRepository.findAllByIdIn(ids);
            Map<UUID, Message> messagesById = new HashMap<>();
            for (Message message : fetchedMessages) {
                messagesById.put(message.getId(), message);
            }

            List<Message> claimedMessages = new ArrayList<>();
            for (UUID id : ids) {
                Message message = messagesById.get(id);
                if (message == null) {
                    continue;
                }
                message.markProcessing();
                claimedMessages.add(message);
            }

            return messageRepository.saveAll(claimedMessages);
        });
    }

    private void processMessage(Message message) {
        Map<String, String> context = Map.of(
                "messageId", message.getId().toString()
        );
        try (AutoCloseable ignored = LogContext.with(context)) {
            logger.info("Message processing started");
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    Message managedMessage = messageRepository.findById(message.getId())
                        .orElseThrow(() -> new NoSuchElementException("Message not found: " + message.getId()));

                    try (AutoCloseable userContext = LogContext.with("userId", managedMessage.getUser().getUserId())) {
                        messageProcessor.process(managedMessage);
                    } catch (Exception exception) {
                        throw new IllegalStateException("Failed to manage message user logging context", exception);
                    }
                });
                updateMessageStatus(message.getId(), MessageStatus.SUCCESS);
                logger.info("Message processing completed status=SUCCESS");
            } catch (RuntimeException exception) {
                logger.error("Message processing failed", exception);
                updateMessageStatus(message.getId(), MessageStatus.FAILED);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to manage logging context", exception);
        }
    }

    private void updateMessageStatus(UUID messageId, MessageStatus messageStatus) {
        transactionTemplate.executeWithoutResult(status -> {
            Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found: " + messageId));

            if (messageStatus == MessageStatus.SUCCESS) {
                message.markSuccess();
                return;
            }

            if (messageStatus == MessageStatus.FAILED) {
                message.markFailed();
                return;
            }

            if (messageStatus == MessageStatus.PROCESSING) {
                message.markProcessing();
            }
        });
    }
}
