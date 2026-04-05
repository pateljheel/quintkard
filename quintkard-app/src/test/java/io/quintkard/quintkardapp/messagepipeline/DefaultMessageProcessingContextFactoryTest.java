package io.quintkard.quintkardapp.messagepipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quintkard.quintkardapp.message.Message;
import io.quintkard.quintkardapp.message.MessageStatus;
import io.quintkard.quintkardapp.user.User;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DefaultMessageProcessingContextFactoryTest {

    private final DefaultMessageProcessingContextFactory factory = new DefaultMessageProcessingContextFactory();

    @Test
    void createsRunScopedContextForMessage() {
        Message message = message();

        MessageProcessingContext context = factory.createContext(message);

        assertEquals(message.getId(), context.messageId());
        assertEquals("admin", context.userId());
        assertNotNull(context.runId());
        assertTrue(context.memoryScope().conversationId().startsWith("msg:" + message.getId() + ":run:"));
        assertTrue(context.memoryScope().conversationId().endsWith(context.runId()));
    }

    private Message message() {
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
        ReflectionTestUtils.setField(message, "id", UUID.fromString("44444444-4444-4444-4444-444444444444"));
        return message;
    }
}
