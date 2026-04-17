package io.quintkard.quintkardapp.messagepipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quintkard.quintkardapp.config.MessageQueueProperties;
import io.quintkard.quintkardapp.message.Message;
import io.quintkard.quintkardapp.message.MessageStatus;
import io.quintkard.quintkardapp.user.User;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

class DatabaseBackedMessageQueueServiceTest {

    private InternalMessageQueueRepository messageQueueRepository;
    private MessageProcessor messageProcessor;
    private ThreadPoolTaskExecutor taskExecutor;
    private DatabaseBackedMessageQueueService queueService;

    @BeforeEach
    void setUp() {
        messageQueueRepository = mock(InternalMessageQueueRepository.class);
        messageProcessor = mock(MessageProcessor.class);
        taskExecutor = mock(ThreadPoolTaskExecutor.class);
        queueService = new DatabaseBackedMessageQueueService(
                messageQueueRepository,
                messageProcessor,
                transactionManager(),
                taskExecutor,
                messageQueueProperties()
        );
    }

    @Test
    void triggerPendingMessageProcessingSubmitsWorkerOnlyOnce() {
        queueService.triggerPendingMessageProcessing();
        queueService.triggerPendingMessageProcessing();

        verify(taskExecutor, times(1)).execute(any(Runnable.class));
    }

    @Test
    void processMessageMarksSuccessWhenProcessorCompletes() {
        Message message = message();
        when(messageQueueRepository.findById(message.getId()))
                .thenReturn(Optional.of(message), Optional.of(message));

        ReflectionTestUtils.invokeMethod(queueService, "processMessage", message);

        verify(messageProcessor).process(message);
        assertEquals(MessageStatus.SUCCESS, message.getStatus());
    }

    @Test
    void processMessageMarksFailedWhenProcessorThrows() {
        Message message = message();
        when(messageQueueRepository.findById(message.getId()))
                .thenReturn(Optional.of(message), Optional.of(message));
        doThrow(new IllegalStateException("boom")).when(messageProcessor).process(message);

        ReflectionTestUtils.invokeMethod(queueService, "processMessage", message);

        verify(messageProcessor).process(message);
        assertEquals(MessageStatus.FAILED, message.getStatus());
    }

    @Test
    void claimPendingMessagesReturnsEmptyWhenNoIdsClaimed() {
        when(messageQueueRepository.claimMessageIdsByStatus(MessageStatus.PENDING.name(), 10)).thenReturn(List.of());

        @SuppressWarnings("unchecked")
        List<Message> claimed = ReflectionTestUtils.invokeMethod(queueService, "claimPendingMessages");

        assertEquals(List.of(), claimed);
        verify(messageQueueRepository, never()).findAllByIdIn(any());
        verify(messageQueueRepository, never()).saveAll(any());
    }

    @Test
    void claimPendingMessagesMarksFetchedMessagesProcessingAndSkipsMissingOnes() {
        Message first = message();
        Message second = message(UUID.fromString("66666666-6666-6666-6666-666666666666"));
        UUID missingId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        List<UUID> claimedIds = List.of(second.getId(), missingId, first.getId());
        when(messageQueueRepository.claimMessageIdsByStatus(MessageStatus.PENDING.name(), 10)).thenReturn(claimedIds);
        when(messageQueueRepository.findAllByIdIn(claimedIds)).thenReturn(List.of(first, second));
        when(messageQueueRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        @SuppressWarnings("unchecked")
        List<Message> claimed = ReflectionTestUtils.invokeMethod(queueService, "claimPendingMessages");

        assertEquals(List.of(second, first), claimed);
        assertEquals(MessageStatus.PROCESSING, first.getStatus());
        assertEquals(MessageStatus.PROCESSING, second.getStatus());
    }

    @Test
    void processNextBatchReturnsZeroWhenNothingClaimed() {
        when(messageQueueRepository.claimMessageIdsByStatus(MessageStatus.PENDING.name(), 10)).thenReturn(List.of());

        Integer processed = ReflectionTestUtils.invokeMethod(queueService, "processNextBatch");

        assertEquals(0, processed);
        verify(messageProcessor, never()).process(any());
    }

    @Test
    void updateMessageStatusMarksProcessingWhenRequested() {
        Message message = message();
        message.markPending();
        when(messageQueueRepository.findById(message.getId())).thenReturn(Optional.of(message));

        ReflectionTestUtils.invokeMethod(
                queueService,
                "updateMessageStatus",
                message.getId(),
                MessageStatus.PROCESSING
        );

        assertEquals(MessageStatus.PROCESSING, message.getStatus());
    }

    @Test
    void drainPendingMessagesResetsWorkerAndRetriggersWhenMessagesRemain() {
        Runnable[] submittedWorker = new Runnable[1];
        doAnswer(invocation -> {
            submittedWorker[0] = invocation.getArgument(0);
            return null;
        }).when(taskExecutor).execute(any(Runnable.class));
        when(messageQueueRepository.claimMessageIdsByStatus(MessageStatus.PENDING.name(), 10)).thenReturn(List.of());
        when(messageQueueRepository.existsByStatus(MessageStatus.PENDING)).thenReturn(true, false);

        queueService.triggerPendingMessageProcessing();
        submittedWorker[0].run();

        verify(taskExecutor, times(2)).execute(any(Runnable.class));
        AtomicBoolean workerRunning = (AtomicBoolean) ReflectionTestUtils.getField(queueService, "workerRunning");
        assertTrue(workerRunning.get());
    }

    @Test
    void drainPendingMessagesDoesNotRetriggerWhenNoMessagesRemain() {
        when(messageQueueRepository.claimMessageIdsByStatus(MessageStatus.PENDING.name(), 10)).thenReturn(List.of());
        when(messageQueueRepository.existsByStatus(MessageStatus.PENDING)).thenReturn(false);

        ReflectionTestUtils.setField(queueService, "workerRunning", new AtomicBoolean(true));

        ReflectionTestUtils.invokeMethod(queueService, "drainPendingMessages");

        verify(taskExecutor, never()).execute(any(Runnable.class));
        AtomicBoolean workerRunning = (AtomicBoolean) ReflectionTestUtils.getField(queueService, "workerRunning");
        assertFalse(workerRunning.get());
    }

    private PlatformTransactionManager transactionManager() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        TransactionStatus transactionStatus = new SimpleTransactionStatus();
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        return transactionManager;
    }

    private MessageQueueProperties messageQueueProperties() {
        MessageQueueProperties properties = new MessageQueueProperties();
        properties.setBatchSize(10);
        return properties;
    }

    private Message message() {
        return message(UUID.fromString("55555555-5555-5555-5555-555555555555"));
    }

    private Message message(UUID id) {
        User user = new User("admin", "Admin", "admin@example.com", "hash", false);
        Message message = new Message(
                user,
                "gmail",
                "ext-1",
                "EMAIL",
                MessageStatus.PENDING,
                "Please follow up on the invoice.",
                "Invoice follow up",
                Map.of(),
                Map.of(),
                Instant.parse("2026-04-05T12:00:00Z"),
                Instant.parse("2026-04-05T11:55:00Z")
        );
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }
}
