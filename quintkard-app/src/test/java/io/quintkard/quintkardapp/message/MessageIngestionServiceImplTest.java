package io.quintkard.quintkardapp.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quintkard.quintkardapp.user.User;
import io.quintkard.quintkardapp.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MessageIngestionServiceImplTest {

    private MessageRepository messageRepository;
    private UserRepository userRepository;
    private MessageIngestionServiceImpl service;

    @BeforeEach
    void setUp() {
        messageRepository = mock(MessageRepository.class);
        userRepository = mock(UserRepository.class);
        service = new MessageIngestionServiceImpl(messageRepository, userRepository);
    }

    @Test
    void ingestMessageBuildsPendingMessageWithSummaryAndNormalizedMaps() {
        User user = new User("admin", "Admin", "admin@example.com", "hash", false);
        when(userRepository.findByUserId("admin")).thenReturn(Optional.of(user));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Message saved = service.ingestMessage("admin", new MessageEnvelope(
                "gmail",
                "ext-1",
                "EMAIL",
                "  Follow up   on the invoice from   August  ",
                Map.of("threadId", "t-1"),
                Map.of("source", "email"),
                Instant.parse("2026-04-05T11:55:00Z")
        ));

        assertSame(user, saved.getUser());
        assertEquals(MessageStatus.PENDING, saved.getStatus());
        assertEquals("gmail", saved.getSourceService());
        assertEquals("ext-1", saved.getExternalMessageId());
        assertEquals("EMAIL", saved.getMessageType());
        assertEquals("  Follow up   on the invoice from   August  ", saved.getPayload());
        assertEquals("Follow up on the invoice from August", saved.getSummary());
        assertEquals(Map.of("threadId", "t-1"), saved.getMetadata());
        assertEquals(Map.of("source", "email"), saved.getDetails());
        assertEquals(Instant.parse("2026-04-05T11:55:00Z"), saved.getSourceCreatedAt());
        verify(messageRepository).save(saved);
    }

    @Test
    void ingestMessageReturnsNullSummaryForBlankPayloadAndNullMaps() {
        User user = new User("admin", "Admin", "admin@example.com", "hash", true);
        when(userRepository.findByUserId("admin")).thenReturn(Optional.of(user));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Message saved = service.ingestMessage("admin", new MessageEnvelope(
                "gmail",
                "ext-1",
                "EMAIL",
                "   ",
                Map.of(),
                null,
                null
        ));

        assertEquals("   ", saved.getPayload());
        assertNull(saved.getSummary());
        assertNull(saved.getMetadata());
        assertNull(saved.getDetails());
    }

    @Test
    void ingestMessageTruncatesLongSummary() {
        User user = new User("admin", "Admin", "admin@example.com", "hash", false);
        String payload = ("Follow up on the invoice from August 2025 and confirm the billing address correction. ").repeat(4);
        when(userRepository.findByUserId("admin")).thenReturn(Optional.of(user));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Message saved = service.ingestMessage("admin", new MessageEnvelope(
                "gmail",
                "ext-1",
                "EMAIL",
                payload,
                null,
                null,
                null
        ));

        assertTrue(saved.getSummary().length() <= 182);
        assertEquals("...", saved.getSummary().substring(saved.getSummary().length() - 3));
    }

    @Test
    void ingestMessagesReturnsEmptyListForNullOrEmptyInput() {
        assertEquals(List.of(), service.ingestMessages("admin", null));
        assertEquals(List.of(), service.ingestMessages("admin", List.of()));
        verifyNoInteractions(userRepository, messageRepository);
    }

    @Test
    void ingestMessagesBuildsAndSavesAllMessages() {
        User user = new User("admin", "Admin", "admin@example.com", "hash", false);
        when(userRepository.findByUserId("admin")).thenReturn(Optional.of(user));
        when(messageRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Message> saved = service.ingestMessages("admin", List.of(
                new MessageEnvelope("gmail", "1", "EMAIL", "first payload", null, null, null),
                new MessageEnvelope("slack", "2", "CHAT", "second payload", null, null, null)
        ));

        assertEquals(2, saved.size());
        assertEquals("first payload", saved.get(0).getSummary());
        assertEquals("second payload", saved.get(1).getSummary());
        verify(messageRepository).saveAll(saved);
    }

    @Test
    void ingestMessageFailsWhenUserMissing() {
        when(userRepository.findByUserId("admin")).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> service.ingestMessage("admin", new MessageEnvelope(
                        "gmail",
                        "ext-1",
                        "EMAIL",
                        "payload",
                        null,
                        null,
                        null
                ))
        );

        assertEquals("User not found: admin", exception.getMessage());
    }
}
