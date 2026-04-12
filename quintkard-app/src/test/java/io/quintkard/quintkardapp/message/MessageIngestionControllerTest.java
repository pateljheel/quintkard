package io.quintkard.quintkardapp.message;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.quintkard.quintkardapp.user.User;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MessageIngestionControllerTest {

    private MessageIngestionService messageIngestionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        messageIngestionService = mock(MessageIngestionService.class);
        MessageIngestionController controller = new MessageIngestionController(messageIngestionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void ingestMessageUsesAuthenticatedUser() throws Exception {
        Message message = message(UUID.randomUUID(), "gmail", "EMAIL");
        when(messageIngestionService.ingestMessage(eq("admin"), eq(new MessageEnvelope(
                "gmail",
                "ext-1",
                "EMAIL",
                "Follow up on invoice",
                Map.of("threadId", "t-1"),
                Map.of("priority", "high"),
                Instant.parse("2026-04-05T11:55:00Z")
        )))).thenReturn(message);

        mockMvc.perform(authorized(post("/api/messages/ingest"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceService": "gmail",
                                  "externalMessageId": "ext-1",
                                  "messageType": "EMAIL",
                                  "payload": "Follow up on invoice",
                                  "metadata": {"threadId": "t-1"},
                                  "details": {"priority": "high"},
                                  "sourceCreatedAt": "2026-04-05T11:55:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("admin"))
                .andExpect(jsonPath("$.sourceService").value("gmail"))
                .andExpect(jsonPath("$.messageType").value("EMAIL"));

        verify(messageIngestionService).ingestMessage(eq("admin"), eq(new MessageEnvelope(
                "gmail",
                "ext-1",
                "EMAIL",
                "Follow up on invoice",
                Map.of("threadId", "t-1"),
                Map.of("priority", "high"),
                Instant.parse("2026-04-05T11:55:00Z")
        )));
    }

    @Test
    void ingestMessagesReturnsCreatedBatchResponse() throws Exception {
        when(messageIngestionService.ingestMessages(eq("admin"), anyList())).thenReturn(List.of(
                message(UUID.randomUUID(), "gmail", "EMAIL"),
                message(UUID.randomUUID(), "slack", "CHANNEL_MESSAGE")
        ));

        mockMvc.perform(authorized(post("/api/messages/ingest/batch"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {
                                    "sourceService": "gmail",
                                    "externalMessageId": "ext-1",
                                    "messageType": "EMAIL",
                                    "payload": "Follow up on invoice"
                                  },
                                  {
                                    "sourceService": "slack",
                                    "externalMessageId": "ext-2",
                                    "messageType": "CHANNEL_MESSAGE",
                                    "payload": "Reminder in channel"
                                  }
                                ]
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].sourceService").value("gmail"))
                .andExpect(jsonPath("$[1].sourceService").value("slack"));

        verify(messageIngestionService).ingestMessages(eq("admin"), anyList());
    }

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder builder) {
        return builder.principal(new TestingAuthenticationToken("admin", "password"));
    }

    private Message message(UUID id, String sourceService, String messageType) {
        Message message = new Message(
                new User("admin", "Admin", "admin@example.com", "hash", false),
                sourceService,
                "ext-" + id,
                messageType,
                MessageStatus.PENDING,
                "Payload",
                "Summary",
                Map.of(),
                Map.of(),
                Instant.parse("2026-04-05T12:00:00Z"),
                Instant.parse("2026-04-05T11:55:00Z")
        );
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }
}
