package io.quintkard.quintkardapp.orchestratorexecution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quintkard.quintkardapp.agent.AgentConfig;
import io.quintkard.quintkardapp.agentexecution.AgentExecutionResult;
import io.quintkard.quintkardapp.agentexecution.AgentExecutionStatus;
import io.quintkard.quintkardapp.aimodel.AiChatService;
import io.quintkard.quintkardapp.aimodel.AiMemoryScope;
import io.quintkard.quintkardapp.aimodel.AiMemoryService;
import io.quintkard.quintkardapp.message.Message;
import io.quintkard.quintkardapp.message.MessageStatus;
import io.quintkard.quintkardapp.messagepipeline.MessageProcessingContext;
import io.quintkard.quintkardapp.messagepipeline.MessageProcessingContextFactory;
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
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class OrchestratorExecutionServiceImplTest {

    private AiChatService aiChatService;
    private AiMemoryService aiMemoryService;
    private AgentDispatchService agentDispatchService;
    private MessageProcessingContextFactory contextFactory;
    private OrchestratorExecutionServiceImpl orchestratorExecutionService;

    @BeforeEach
    void setUp() {
        aiChatService = mock(AiChatService.class);
        aiMemoryService = mock(AiMemoryService.class);
        agentDispatchService = mock(AgentDispatchService.class);
        contextFactory = mock(MessageProcessingContextFactory.class);
        orchestratorExecutionService =
                new OrchestratorExecutionServiceImpl(aiChatService, aiMemoryService, agentDispatchService, contextFactory);
    }

    @Test
    void skipsDispatchWhenFilteringRejectsMessage() {
        Message message = message();
        OrchestratorConfig config = config(Set.of(agent("Finance"), agent("Ops")));
        when(contextFactory.createContext(message)).thenReturn(context());
        when(aiChatService.chatForObject(any())).thenReturn(new FilteringDecision(false, "spam"));

        OrchestratorExecutionResult result = orchestratorExecutionService.execute(config, message);

        assertEquals(false, result.filteringDecision().accepted());
        assertEquals("Message rejected by filtering step", result.routingDecision().reason());
        assertTrue(result.agentResults().isEmpty());
        verify(agentDispatchService, never()).dispatch(any(), any(), any(), any());
        verify(aiMemoryService).clear(context().memoryScope());
    }

    @Test
    void filtersRoutingDecisionToConfiguredDistinctActiveAgents() {
        AgentConfig finance = agent("Finance");
        AgentConfig ops = agent("Ops");
        AgentConfig ignored = agent("Ignored");
        Message message = message();
        OrchestratorConfig config = config(Set.of(finance, ops));
        MessageProcessingContext context = context();
        when(contextFactory.createContext(message)).thenReturn(context);
        when(aiChatService.chatForObject(any()))
                .thenReturn(new FilteringDecision(true, "accepted"))
                .thenReturn(new RoutingDecision(
                        List.of(finance.getId(), ignored.getId(), finance.getId(), ops.getId()),
                        "matched multiple workflows"
                ));
        when(agentDispatchService.dispatch(any(), any(), any(), any())).thenReturn(Map.of(
                finance.getId(),
                new AgentExecutionResult("done", AgentExecutionStatus.SUCCESS, 1, 0, List.of())
        ));

        OrchestratorExecutionResult result = orchestratorExecutionService.execute(config, message);

        assertEquals(List.of(finance.getId(), ops.getId()), result.routingDecision().agentIds());

        ArgumentCaptor<RoutingDecision> routingCaptor = ArgumentCaptor.forClass(RoutingDecision.class);
        verify(agentDispatchService).dispatch(any(), routingCaptor.capture(), any(), any());
        assertEquals(List.of(finance.getId(), ops.getId()), routingCaptor.getValue().agentIds());
        verify(aiMemoryService).clear(context.memoryScope());
    }

    @Test
    void usesDisabledFilteringDecisionWhenFilteringPromptBlank() {
        AgentConfig finance = agent("Finance");
        Message message = message();
        OrchestratorConfig config = new OrchestratorConfig(
                new User("admin", "Admin", "admin@example.com", "hash", false),
                "   ",
                "gemini-2.5-flash",
                "Route the message",
                "gemini-2.5-flash",
                Set.of(finance)
        );
        when(contextFactory.createContext(message)).thenReturn(context());
        when(aiChatService.chatForObject(any()))
                .thenReturn(new RoutingDecision(List.of(finance.getId()), "selected"));
        when(agentDispatchService.dispatch(any(), any(), any(), any())).thenReturn(Map.of());

        OrchestratorExecutionResult result = orchestratorExecutionService.execute(config, message);

        assertTrue(result.filteringDecision().accepted());
        assertEquals("Filtering disabled", result.filteringDecision().reason());
        verify(aiChatService).chatForObject(any());
        verify(aiMemoryService).clear(context().memoryScope());
    }

    @Test
    void usesDisabledFilteringDecisionWhenFilteringPromptNull() {
        AgentConfig finance = agent("Finance");
        Message message = message();
        OrchestratorConfig config = new OrchestratorConfig(
                new User("admin", "Admin", "admin@example.com", "hash", false),
                null,
                "gemini-2.5-flash",
                "Route the message",
                "gemini-2.5-flash",
                Set.of(finance)
        );
        when(contextFactory.createContext(message)).thenReturn(context());
        when(aiChatService.chatForObject(any()))
                .thenReturn(new RoutingDecision(List.of(finance.getId()), "selected"));
        when(agentDispatchService.dispatch(any(), any(), any(), any())).thenReturn(Map.of());

        OrchestratorExecutionResult result = orchestratorExecutionService.execute(config, message);

        assertTrue(result.filteringDecision().accepted());
        assertEquals("Filtering disabled", result.filteringDecision().reason());
        verify(aiChatService).chatForObject(any());
        verify(aiMemoryService).clear(context().memoryScope());
    }

    @Test
    void returnsNoActiveAgentsDecisionWithoutCallingRoutingModel() {
        Message message = message();
        OrchestratorConfig config = config(Set.of());
        when(contextFactory.createContext(message)).thenReturn(context());
        when(aiChatService.chatForObject(any())).thenReturn(new FilteringDecision(true, "accepted"));
        when(agentDispatchService.dispatch(any(), any(), any(), any())).thenReturn(Map.of());

        OrchestratorExecutionResult result = orchestratorExecutionService.execute(config, message);

        assertEquals(List.of(), result.routingDecision().agentIds());
        assertEquals("No active agents configured", result.routingDecision().reason());
        verify(agentDispatchService).dispatch(any(), any(), any(), any());
        verify(aiMemoryService).clear(context().memoryScope());
    }

    @Test
    void normalizesNullRoutingAgentIdsAndBlankReason() {
        AgentConfig finance = agent("Finance");
        Message message = message();
        OrchestratorConfig config = config(Set.of(finance));
        when(contextFactory.createContext(message)).thenReturn(context());
        when(aiChatService.chatForObject(any()))
                .thenReturn(new FilteringDecision(true, "accepted"))
                .thenReturn(new RoutingDecision(null, "   "));
        when(agentDispatchService.dispatch(any(), any(), any(), any())).thenReturn(Map.of());

        OrchestratorExecutionResult result = orchestratorExecutionService.execute(config, message);

        assertEquals(List.of(), result.routingDecision().agentIds());
        assertEquals("No routing reason provided", result.routingDecision().reason());
        verify(aiMemoryService).clear(context().memoryScope());
    }

    @Test
    void normalizesNullRoutingReasonWhenAgentIdsProvided() {
        AgentConfig finance = agent("Finance");
        Message message = message();
        OrchestratorConfig config = config(Set.of(finance));
        when(contextFactory.createContext(message)).thenReturn(context());
        when(aiChatService.chatForObject(any()))
                .thenReturn(new FilteringDecision(true, "accepted"))
                .thenReturn(new RoutingDecision(List.of(finance.getId()), null));
        when(agentDispatchService.dispatch(any(), any(), any(), any())).thenReturn(Map.of());

        OrchestratorExecutionResult result = orchestratorExecutionService.execute(config, message);

        assertEquals(List.of(finance.getId()), result.routingDecision().agentIds());
        assertEquals("No routing reason provided", result.routingDecision().reason());
        verify(aiMemoryService).clear(context().memoryScope());
    }

    @Test
    void wrapsLoggingContextFailureWhenContextRunIdNull() {
        MessageProcessingContext invalidContext = new MessageProcessingContext(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "admin",
                null,
                new AiMemoryScope("msg:1:run:42")
        );
        when(contextFactory.createContext(any())).thenReturn(invalidContext);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orchestratorExecutionService.execute(config(Set.of(agent("Finance"))), message())
        );

        assertEquals("Failed to manage orchestration logging context", exception.getMessage());
        verify(aiMemoryService).clear(invalidContext.memoryScope());
    }

    @Test
    void routingContextIncludesAgentDescriptionsAndMessageContext() {
        AgentConfig finance = agent("Finance");
        AgentConfig ops = agent("Ops");
        Message message = message();
        OrchestratorConfig config = config(Set.of(finance, ops));
        when(contextFactory.createContext(message)).thenReturn(context());
        when(aiChatService.chatForObject(any()))
                .thenReturn(new FilteringDecision(true, "accepted"))
                .thenReturn(new RoutingDecision(List.of(finance.getId()), "selected"));
        when(agentDispatchService.dispatch(any(), any(), any(), any())).thenReturn(Map.of());

        orchestratorExecutionService.execute(config, message);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<io.quintkard.quintkardapp.aimodel.AiStructuredChatRequest> requestCaptor =
                ArgumentCaptor.forClass(io.quintkard.quintkardapp.aimodel.AiStructuredChatRequest.class);
        verify(aiChatService, org.mockito.Mockito.times(2)).chatForObject(requestCaptor.capture());
        @SuppressWarnings("unchecked")
        List<io.quintkard.quintkardapp.aimodel.AiStructuredChatRequest<?>> requests =
                (List<io.quintkard.quintkardapp.aimodel.AiStructuredChatRequest<?>>) (List<?>) requestCaptor.getAllValues();
        String routingPrompt = requests.get(1).messages().get(1).content();
        assertTrue(routingPrompt.contains("Available agents:"));
        assertTrue(routingPrompt.contains("Name: Finance"));
        assertTrue(routingPrompt.contains("Description: Finance description"));
        assertTrue(routingPrompt.contains("Payload:\nPlease follow up on the invoice from last August."));
        verify(aiMemoryService).clear(context().memoryScope());
    }

    @Test
    void routingContextUsesBlankSummaryAndPayloadWhenMessageFieldsNull() {
        AgentConfig finance = agent("Finance");
        Message message = messageWithNullSummaryAndPayload();
        OrchestratorConfig config = config(Set.of(finance));
        when(contextFactory.createContext(message)).thenReturn(context());
        when(aiChatService.chatForObject(any()))
                .thenReturn(new FilteringDecision(true, "accepted"))
                .thenReturn(new RoutingDecision(List.of(finance.getId()), "selected"));
        when(agentDispatchService.dispatch(any(), any(), any(), any())).thenReturn(Map.of());

        orchestratorExecutionService.execute(config, message);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<io.quintkard.quintkardapp.aimodel.AiStructuredChatRequest> requestCaptor =
                ArgumentCaptor.forClass(io.quintkard.quintkardapp.aimodel.AiStructuredChatRequest.class);
        verify(aiChatService, org.mockito.Mockito.times(2)).chatForObject(requestCaptor.capture());
        @SuppressWarnings("unchecked")
        List<io.quintkard.quintkardapp.aimodel.AiStructuredChatRequest<?>> requests =
                (List<io.quintkard.quintkardapp.aimodel.AiStructuredChatRequest<?>>) (List<?>) requestCaptor.getAllValues();
        String filteringPrompt = requests.get(0).messages().get(1).content();
        assertTrue(filteringPrompt.contains("Summary: \n"));
        assertTrue(filteringPrompt.contains("Payload:\n"));
        verify(aiMemoryService).clear(context().memoryScope());
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

    private Message messageWithNullSummaryAndPayload() {
        User user = new User("admin", "Admin", "admin@example.com", "hash", false);
        Message message = new Message(
                user,
                "gmail",
                "ext-1",
                "EMAIL",
                MessageStatus.PENDING,
                null,
                null,
                Map.of(),
                Map.of(),
                Instant.parse("2026-04-05T12:00:00Z"),
                Instant.parse("2026-04-05T11:55:00Z")
        );
        ReflectionTestUtils.setField(message, "id", UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
        return message;
    }
}
