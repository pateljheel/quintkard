package io.quintkard.quintkardapp.messagepipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quintkard.quintkardapp.message.Message;
import io.quintkard.quintkardapp.message.MessageStatus;
import io.quintkard.quintkardapp.orchestrator.OrchestratorConfig;
import io.quintkard.quintkardapp.orchestrator.OrchestratorService;
import io.quintkard.quintkardapp.orchestratorexecution.FilteringDecision;
import io.quintkard.quintkardapp.orchestratorexecution.OrchestratorExecutionResult;
import io.quintkard.quintkardapp.orchestratorexecution.OrchestratorExecutionService;
import io.quintkard.quintkardapp.orchestratorexecution.RoutingDecision;
import io.quintkard.quintkardapp.user.User;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

class DefaultMessageProcessorTest {

    private OrchestratorService orchestratorService;
    private OrchestratorExecutionService orchestratorExecutionService;
    private DefaultMessageProcessor defaultMessageProcessor;

    @BeforeEach
    void setUp() {
        orchestratorService = mock(OrchestratorService.class);
        orchestratorExecutionService = mock(OrchestratorExecutionService.class);
        defaultMessageProcessor = new DefaultMessageProcessor(
                orchestratorService,
                orchestratorExecutionService,
                new ObjectMapper()
        );
    }

    @Test
    void skipsOrchestrationWhenRoutingConfigMissing() {
        Message message = message();
        when(orchestratorService.getConfig("admin")).thenReturn(new OrchestratorConfig(
                message.getUser(),
                "Filter",
                "gemini-2.5-flash",
                "   ",
                "gemini-2.5-flash",
                Set.of()
        ));

        defaultMessageProcessor.process(message);

        verify(orchestratorExecutionService, never()).execute(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertNotNull(message.getDetails());
        @SuppressWarnings("unchecked")
        Map<String, Object> orchestration = (Map<String, Object>) message.getDetails().get("orchestration");
        assertEquals("SKIPPED", orchestration.get("status"));
        assertEquals("No routing configuration defined", orchestration.get("reason"));
    }

    @Test
    void storesOrchestrationResultIntoMessageDetails() {
        Message message = message();
        OrchestratorConfig config = new OrchestratorConfig(
                message.getUser(),
                "Filter",
                "gemini-2.5-flash",
                "Route",
                "gemini-2.5-flash",
                Set.of()
        );
        OrchestratorExecutionResult result = new OrchestratorExecutionResult(
                new FilteringDecision(true, "accepted"),
                new RoutingDecision(List.of(UUID.fromString("11111111-1111-1111-1111-111111111111")), "matched"),
                Map.of()
        );
        when(orchestratorService.getConfig("admin")).thenReturn(config);
        when(orchestratorExecutionService.execute(config, message)).thenReturn(result);

        defaultMessageProcessor.process(message);

        verify(orchestratorExecutionService).execute(config, message);
        assertNotNull(message.getDetails());
        @SuppressWarnings("unchecked")
        Map<String, Object> orchestration = (Map<String, Object>) message.getDetails().get("orchestration");
        assertTrue(orchestration.containsKey("filteringDecision"));
        assertTrue(orchestration.containsKey("routingDecision"));
        @SuppressWarnings("unchecked")
        Map<String, Object> filteringDecision = (Map<String, Object>) orchestration.get("filteringDecision");
        assertEquals(true, filteringDecision.get("accepted"));
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
        ReflectionTestUtils.setField(message, "id", UUID.fromString("22222222-2222-2222-2222-222222222222"));
        return message;
    }
}
