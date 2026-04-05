package io.quintkard.quintkardapp.orchestratorexecution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quintkard.quintkardapp.agent.AgentConfig;
import io.quintkard.quintkardapp.agentexecution.AgentExecutionResult;
import io.quintkard.quintkardapp.agentexecution.AgentExecutionService;
import io.quintkard.quintkardapp.agentexecution.AgentExecutionStatus;
import io.quintkard.quintkardapp.aimodel.AiMemoryScope;
import io.quintkard.quintkardapp.message.Message;
import io.quintkard.quintkardapp.message.MessageStatus;
import io.quintkard.quintkardapp.messagepipeline.MessageProcessingContext;
import io.quintkard.quintkardapp.orchestrator.OrchestratorConfig;
import io.quintkard.quintkardapp.user.User;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AgentDispatchServiceImplTest {

    private AgentExecutionService agentExecutionService;
    private AgentDispatchServiceImpl agentDispatchService;

    @BeforeEach
    void setUp() {
        agentExecutionService = mock(AgentExecutionService.class);
        agentDispatchService = new AgentDispatchServiceImpl(agentExecutionService);
    }

    @Test
    void returnsEmptyResultsWhenRoutingAgentIdsNull() {
        Map<UUID, AgentExecutionResult> result = agentDispatchService.dispatch(
                config(Set.of(agent("Finance"))),
                new RoutingDecision(null, "none"),
                context(),
                message()
        );

        assertTrue(result.isEmpty());
        verify(agentExecutionService, never()).execute(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsEmptyResultsWhenRoutingAgentIdsEmpty() {
        Map<UUID, AgentExecutionResult> result = agentDispatchService.dispatch(
                config(Set.of(agent("Finance"))),
                new RoutingDecision(List.of(), "none"),
                context(),
                message()
        );

        assertTrue(result.isEmpty());
        verify(agentExecutionService, never()).execute(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void dispatchesOnlyConfiguredAgentsAndPreservesRoutingOrder() {
        AgentConfig finance = agent("Finance");
        AgentConfig ops = agent("Ops");
        UUID ignoredId = UUID.randomUUID();
        when(agentExecutionService.execute(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(finance), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AgentExecutionResult("finance", AgentExecutionStatus.SUCCESS, 1, 0, List.of()));
        when(agentExecutionService.execute(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(ops), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AgentExecutionResult("ops", AgentExecutionStatus.SUCCESS, 1, 0, List.of()));

        Map<UUID, AgentExecutionResult> result = agentDispatchService.dispatch(
                config(Set.of(finance, ops)),
                new RoutingDecision(List.of(ops.getId(), ignoredId, finance.getId()), "route"),
                context(),
                message()
        );

        assertEquals(List.of(ops.getId(), finance.getId()), List.copyOf(result.keySet()));
        assertEquals("ops", result.get(ops.getId()).responseText());
        assertEquals("finance", result.get(finance.getId()).responseText());
    }

    @Test
    void wrapsExceptionsThrownWhileDispatchingAnAgent() {
        AgentConfig invalid = agent("Broken");
        when(agentExecutionService.execute(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(invalid), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("boom"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> agentDispatchService.dispatch(
                        config(Set.of(invalid)),
                        new RoutingDecision(List.of(invalid.getId()), "route"),
                        context(),
                        message()
                )
        );

        assertEquals("Failed to manage agent logging context", exception.getMessage());
    }

    private MessageProcessingContext context() {
        return new MessageProcessingContext(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "admin",
                "run-42",
                new AiMemoryScope("msg:1:run:42")
        );
    }

    private AgentConfig agent(String name) {
        AgentConfig agent = new AgentConfig(
                new User("admin", "Admin", "admin@example.com", "hash", false),
                name,
                name + " description",
                "Prompt for " + name,
                "gemini-2.5-flash",
                0.2
        );
        ReflectionTestUtils.setField(agent, "id", UUID.randomUUID());
        return agent;
    }

    private OrchestratorConfig config(Set<AgentConfig> activeAgents) {
        return new OrchestratorConfig(
                new User("admin", "Admin", "admin@example.com", "hash", false),
                "Decide whether to accept the message",
                "gemini-2.5-flash",
                "Route the message",
                "gemini-2.5-flash",
                new LinkedHashSet<>(activeAgents)
        );
    }

    private Message message() {
        User user = new User("admin", "Admin", "admin@example.com", "hash", false);
        Message message = new Message(
                user,
                "gmail",
                "ext-1",
                "EMAIL",
                MessageStatus.PENDING,
                "Please follow up on the invoice from last August.",
                "Invoice follow up",
                Map.of(),
                Map.of(),
                Instant.parse("2026-04-05T12:00:00Z"),
                Instant.parse("2026-04-05T11:55:00Z")
        );
        ReflectionTestUtils.setField(message, "id", UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        return message;
    }
}
