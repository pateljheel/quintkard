package io.quintkard.quintkardapp.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quintkard.quintkardapp.user.User;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

class MessageServiceImplTest {

    private MessageRepository messageRepository;
    private MessageServiceImpl service;

    @BeforeEach
    void setUp() {
        messageRepository = mock(MessageRepository.class);
        service = new MessageServiceImpl(messageRepository);
    }

    @Test
    void getMessageReturnsUserScopedMessage() {
        UUID messageId = UUID.randomUUID();
        Message message = message(messageId, MessageStatus.PENDING);
        when(messageRepository.findByIdAndUserUserId(messageId, "admin")).thenReturn(Optional.of(message));

        Message result = service.getMessage("admin", messageId);

        assertSame(message, result);
    }

    @Test
    void getMessageThrowsWhenMissing() {
        UUID messageId = UUID.randomUUID();
        when(messageRepository.findByIdAndUserUserId(messageId, "admin")).thenReturn(Optional.empty());

        MessageNotFoundException exception = assertThrows(
                MessageNotFoundException.class,
                () -> service.getMessage("admin", messageId)
        );

        assertEquals("Message not found: " + messageId, exception.getMessage());
    }

    @Test
    void listMessagesWithoutQueryUsesDefaultRepositoryPath() {
        @SuppressWarnings("unchecked")
        Slice<MessageSummaryProjection> expected = new SliceImpl<>(List.of(mock(MessageSummaryProjection.class)));
        when(messageRepository.findSummariesByFiltersOrderByIngestedAtDesc(
                eq("admin"),
                eq(null),
                eq("gmail"),
                eq("EMAIL"),
                eq(Instant.parse("2026-04-05T00:00:00Z")),
                eq(Instant.parse("2026-04-06T00:00:00Z")),
                any(PageRequest.class)
        ))
                .thenReturn(expected);

        Slice<MessageSummaryProjection> result = service.listMessages(
                "admin",
                0,
                25,
                "   ",
                null,
                "gmail",
                "EMAIL",
                Instant.parse("2026-04-05T00:00:00Z"),
                Instant.parse("2026-04-06T00:00:00Z")
        );

        assertSame(expected, result);
        verify(messageRepository).findSummariesByFiltersOrderByIngestedAtDesc(
                "admin",
                null,
                "gmail",
                "EMAIL",
                Instant.parse("2026-04-05T00:00:00Z"),
                Instant.parse("2026-04-06T00:00:00Z"),
                PageRequest.of(0, 25)
        );
    }

    @Test
    void listMessagesWithoutQueryAndWithStatusUsesStatusRepositoryPath() {
        @SuppressWarnings("unchecked")
        Slice<MessageSummaryProjection> expected = new SliceImpl<>(List.of(mock(MessageSummaryProjection.class)));
        when(messageRepository.findSummariesByFiltersOrderByIngestedAtDesc(
                eq("admin"),
                eq(MessageStatus.FAILED),
                eq("slack"),
                eq("CHANNEL_MESSAGE"),
                eq(Instant.parse("2026-04-05T00:00:00Z")),
                eq(Instant.parse("2026-04-06T00:00:00Z")),
                any(PageRequest.class)
        ))
                .thenReturn(expected);

        Slice<MessageSummaryProjection> result = service.listMessages(
                "admin",
                0,
                -1,
                null,
                MessageStatus.FAILED,
                " slack ",
                " CHANNEL_MESSAGE ",
                Instant.parse("2026-04-05T00:00:00Z"),
                Instant.parse("2026-04-06T00:00:00Z")
        );

        assertSame(expected, result);
        verify(messageRepository).findSummariesByFiltersOrderByIngestedAtDesc(
                "admin",
                MessageStatus.FAILED,
                "slack",
                "CHANNEL_MESSAGE",
                Instant.parse("2026-04-05T00:00:00Z"),
                Instant.parse("2026-04-06T00:00:00Z"),
                PageRequest.of(0, 20)
        );
    }

    @Test
    void listMessagesWithQueryUsesSearchRepositoryPath() {
        @SuppressWarnings("unchecked")
        Slice<MessageSummaryProjection> expected = new SliceImpl<>(List.of(mock(MessageSummaryProjection.class)));
        when(messageRepository.searchSummariesByUserId(
                eq("admin"),
                eq("SUCCESS"),
                eq("gmail"),
                eq("EMAIL"),
                eq(Instant.parse("2026-04-05T00:00:00Z")),
                eq(Instant.parse("2026-04-06T00:00:00Z")),
                eq("invoice"),
                any(PageRequest.class)
        ))
                .thenReturn(expected);

        Slice<MessageSummaryProjection> result = service.listMessages(
                "admin",
                -5,
                500,
                "  invoice  ",
                MessageStatus.SUCCESS,
                "gmail",
                "EMAIL",
                Instant.parse("2026-04-05T00:00:00Z"),
                Instant.parse("2026-04-06T00:00:00Z")
        );

        assertSame(expected, result);
        verify(messageRepository).searchSummariesByUserId(
                "admin",
                "SUCCESS",
                "gmail",
                "EMAIL",
                Instant.parse("2026-04-05T00:00:00Z"),
                Instant.parse("2026-04-06T00:00:00Z"),
                "invoice",
                PageRequest.of(0, 100)
        );
    }

    @Test
    void updateMessageStatusAppliesPendingStatus() {
        assertStatusUpdate(MessageStatus.PROCESSING, MessageStatus.PENDING);
    }

    @Test
    void updateMessageStatusAppliesProcessingStatus() {
        assertStatusUpdate(MessageStatus.PENDING, MessageStatus.PROCESSING);
    }

    @Test
    void updateMessageStatusAppliesFailedStatus() {
        assertStatusUpdate(MessageStatus.PENDING, MessageStatus.FAILED);
    }

    @Test
    void updateMessageStatusAppliesSuccessStatus() {
        assertStatusUpdate(MessageStatus.PENDING, MessageStatus.SUCCESS);
    }

    @Test
    void deleteMessageDeletesResolvedEntity() {
        UUID messageId = UUID.randomUUID();
        Message message = message(messageId, MessageStatus.PENDING);
        when(messageRepository.findByIdAndUserUserId(messageId, "admin")).thenReturn(Optional.of(message));

        service.deleteMessage("admin", messageId);

        verify(messageRepository).delete(message);
    }

    private void assertStatusUpdate(MessageStatus initialStatus, MessageStatus targetStatus) {
        UUID messageId = UUID.randomUUID();
        Message message = message(messageId, initialStatus);
        when(messageRepository.findByIdAndUserUserId(messageId, "admin")).thenReturn(Optional.of(message));

        Message updated = service.updateMessageStatus("admin", messageId, targetStatus);

        assertSame(message, updated);
        assertEquals(targetStatus, updated.getStatus());
    }

    private Message message(UUID id, MessageStatus status) {
        Message message = new Message(
                new User("admin", "Admin", "admin@example.com", "hash", false),
                "gmail",
                "ext-1",
                "EMAIL",
                status,
                "Please follow up",
                "Follow up",
                Map.of(),
                Map.of(),
                Instant.parse("2026-04-05T12:00:00Z"),
                Instant.parse("2026-04-05T11:55:00Z")
        );
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }
}
