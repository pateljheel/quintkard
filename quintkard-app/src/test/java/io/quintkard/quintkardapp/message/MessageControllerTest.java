package io.quintkard.quintkardapp.message;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.quintkard.quintkardapp.user.User;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MessageControllerTest {

    private MessageService messageService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        messageService = mock(MessageService.class);
        MessageController controller = new MessageController(messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
    }

    @Test
    void listMessagesUsesAuthenticatedUser() throws Exception {
        UUID messageId = UUID.randomUUID();
        MessageSummaryProjection summary = mock(MessageSummaryProjection.class);
        when(summary.getId()).thenReturn(messageId);
        when(summary.getUserId()).thenReturn("admin");
        when(summary.getSourceService()).thenReturn("gmail");
        when(summary.getExternalMessageId()).thenReturn("ext-1");
        when(summary.getMessageType()).thenReturn("EMAIL");
        when(summary.getStatus()).thenReturn(MessageStatus.PENDING);
        when(summary.getSummary()).thenReturn("Invoice follow up");
        when(summary.getIngestedAt()).thenReturn(Instant.parse("2026-04-05T12:00:00Z"));
        when(summary.getSourceCreatedAt()).thenReturn(Instant.parse("2026-04-05T11:55:00Z"));
        when(messageService.listMessages(
                "admin",
                0,
                20,
                "invoice",
                MessageStatus.PENDING,
                "gmail",
                "EMAIL",
                Instant.parse("2026-04-05T00:00:00Z"),
                Instant.parse("2026-04-06T00:00:00Z")
        ))
                .thenReturn(new SliceImpl<>(java.util.List.of(summary), PageRequest.of(0, 20), false));

        mockMvc.perform(authorized(get("/api/messages"))
                        .param("query", "invoice")
                        .param("status", "PENDING")
                        .param("sourceService", "gmail")
                        .param("messageType", "EMAIL")
                        .param("ingestedAfter", "2026-04-05T00:00:00Z")
                        .param("ingestedBefore", "2026-04-06T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(messageId.toString()))
                .andExpect(jsonPath("$.items[0].sourceService").value("gmail"));

        verify(messageService).listMessages(
                "admin",
                0,
                20,
                "invoice",
                MessageStatus.PENDING,
                "gmail",
                "EMAIL",
                Instant.parse("2026-04-05T00:00:00Z"),
                Instant.parse("2026-04-06T00:00:00Z")
        );
    }

    @Test
    void updateMessageStatusReturnsUpdatedMessage() throws Exception {
        UUID messageId = UUID.randomUUID();
        Message message = new Message(
                new User("admin", "Admin", "admin@example.com", "hash", false),
                "gmail",
                "ext-1",
                "EMAIL",
                MessageStatus.SUCCESS,
                "Please follow up",
                "Follow up",
                Map.of("threadId", "t-1"),
                Map.of("orchestration", Map.of("status", "DONE")),
                Instant.parse("2026-04-05T12:00:00Z"),
                Instant.parse("2026-04-05T11:55:00Z")
        );
        ReflectionTestUtils.setField(message, "id", messageId);
        when(messageService.updateMessageStatus("admin", messageId, MessageStatus.SUCCESS)).thenReturn(message);

        mockMvc.perform(authorized(patch("/api/messages/{messageId}/status", messageId))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "SUCCESS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(messageId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.details.orchestration.status").value("DONE"));
    }

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder builder) {
        return builder.principal(new TestingAuthenticationToken("admin", "password"));
    }
}
