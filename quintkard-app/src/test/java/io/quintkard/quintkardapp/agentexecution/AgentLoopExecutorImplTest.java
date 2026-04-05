package io.quintkard.quintkardapp.agentexecution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quintkard.quintkardapp.agent.AgentConfig;
import io.quintkard.quintkardapp.agenttool.AiTool;
import io.quintkard.quintkardapp.agenttool.AiToolExecutionRequest;
import io.quintkard.quintkardapp.agenttool.AiToolScopeResolver;
import io.quintkard.quintkardapp.aimodel.AiChatRequest;
import io.quintkard.quintkardapp.aimodel.AiChatResponse;
import io.quintkard.quintkardapp.aimodel.AiMemoryScope;
import io.quintkard.quintkardapp.aimodel.AiMessage;
import io.quintkard.quintkardapp.aimodel.AiMessageRole;
import io.quintkard.quintkardapp.aimodel.AiToolCall;
import io.quintkard.quintkardapp.aimodel.AiToolScope;
import io.quintkard.quintkardapp.aimodel.AiChatService;
import io.quintkard.quintkardapp.message.Message;
import io.quintkard.quintkardapp.message.MessageStatus;
import io.quintkard.quintkardapp.messagepipeline.MessageProcessingContext;
import io.quintkard.quintkardapp.user.User;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

class AgentLoopExecutorImplTest {

    private AiChatService aiChatService;
    private AiToolScopeResolver aiToolScopeResolver;
    private AgentLoopExecutorImpl agentLoopExecutor;

    @BeforeEach
    void setUp() {
        aiChatService = mock(AiChatService.class);
        aiToolScopeResolver = mock(AiToolScopeResolver.class);
        agentLoopExecutor = new AgentLoopExecutorImpl(aiChatService, aiToolScopeResolver, new ObjectMapper());
    }

    @Test
    void returnsSuccessImmediatelyWhenModelFinishesWithoutTools() {
        when(aiChatService.chat(any(AiChatRequest.class)))
                .thenReturn(new AiChatResponse("done", List.of(), true));

        AgentExecutionResult result = agentLoopExecutor.execute(
                context(),
                agentConfig(),
                new AgentExecutionRequest(message(), new AiToolScope(Set.of("clock"))),
                new AgentLoopPolicy(5, 5)
        );

        assertEquals(AgentExecutionStatus.SUCCESS, result.status());
        assertEquals(1, result.iterations());
        assertEquals(0, result.toolCalls());

        ArgumentCaptor<AiChatRequest> requestCaptor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiChatService).chat(requestCaptor.capture());
        List<AiMessage> messages = requestCaptor.getValue().messages();
        assertEquals(2, messages.size());
        assertEquals(AiMessageRole.SYSTEM, messages.getFirst().role());
        assertEquals("You are the finance agent.", messages.getFirst().content());
    }

    @Test
    void executesToolCallsAndBatchesToolResponsesIntoNextTurn() {
        AiTool tool = mock(AiTool.class);
        when(tool.name()).thenReturn("get_current_time");
        when(tool.execute(any(AiToolExecutionRequest.class))).thenReturn(Map.of("currentTime", "2026-04-05T12:00:00Z"));
        when(aiToolScopeResolver.resolveTools("admin", Set.of("get_current_time"))).thenReturn(List.of(tool));
        when(aiChatService.chat(any(AiChatRequest.class)))
                .thenReturn(new AiChatResponse(
                        "",
                        List.of(new AiToolCall("get_current_time", Map.of("timezone", "UTC"))),
                        false
                ))
                .thenReturn(new AiChatResponse("final answer", List.of(), true));

        AgentExecutionResult result = agentLoopExecutor.execute(
                context(),
                agentConfig(),
                new AgentExecutionRequest(message(), new AiToolScope(Set.of("get_current_time"))),
                new AgentLoopPolicy(5, 5)
        );

        assertEquals(AgentExecutionStatus.SUCCESS, result.status());
        assertEquals(2, result.iterations());
        assertEquals(1, result.toolCalls());
        assertEquals(1, result.toolResults().size());

        ArgumentCaptor<AiChatRequest> requestCaptor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiChatService, org.mockito.Mockito.times(2)).chat(requestCaptor.capture());
        AiChatRequest secondRequest = requestCaptor.getAllValues().get(1);
        assertEquals(1, secondRequest.messages().size());
        assertEquals(AiMessageRole.TOOL, secondRequest.messages().getFirst().role());
        assertTrue(secondRequest.messages().getFirst().content().contains("\"toolName\":\"get_current_time\""));
        assertTrue(secondRequest.messages().getFirst().content().contains("\"currentTime\":\"2026-04-05T12:00:00Z\""));
    }

    @Test
    void convertsToolExceptionsIntoRecoverableErrorPayload() {
        AiTool tool = mock(AiTool.class);
        when(tool.name()).thenReturn("create_card");
        when(tool.execute(any(AiToolExecutionRequest.class))).thenThrow(new IllegalArgumentException("Card type is required"));
        when(aiToolScopeResolver.resolveTools("admin", Set.of("create_card"))).thenReturn(List.of(tool));
        when(aiChatService.chat(any(AiChatRequest.class)))
                .thenReturn(new AiChatResponse(
                        "",
                        List.of(new AiToolCall("create_card", Map.of("title", "Follow up"))),
                        false
                ))
                .thenReturn(new AiChatResponse("recovered", List.of(), true));

        AgentExecutionResult result = agentLoopExecutor.execute(
                context(),
                agentConfig(),
                new AgentExecutionRequest(message(), new AiToolScope(Set.of("create_card"))),
                new AgentLoopPolicy(5, 5)
        );

        assertEquals(AgentExecutionStatus.SUCCESS, result.status());
        assertEquals(1, result.toolResults().size());
        assertTrue(String.valueOf(result.toolResults().getFirst().result()).contains("Card type is required"));

        ArgumentCaptor<AiChatRequest> requestCaptor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiChatService, org.mockito.Mockito.times(2)).chat(requestCaptor.capture());
        String toolPayload = requestCaptor.getAllValues().get(1).messages().getFirst().content();
        assertTrue(toolPayload.contains("\"error\":\"Card type is required\""));
    }

    @Test
    void returnsMaxToolCallsReachedWhenNextToolBatchWouldExceedLimit() {
        when(aiToolScopeResolver.resolveTools("admin", Set.of("get_current_time", "get_current_date")))
                .thenReturn(List.of());
        when(aiChatService.chat(any(AiChatRequest.class)))
                .thenReturn(new AiChatResponse(
                        "needs tools",
                        List.of(
                                new AiToolCall("get_current_time", Map.of()),
                                new AiToolCall("get_current_date", Map.of())
                        ),
                        false
                ));

        AgentExecutionResult result = agentLoopExecutor.execute(
                context(),
                agentConfig(),
                new AgentExecutionRequest(message(), new AiToolScope(Set.of("get_current_time", "get_current_date"))),
                new AgentLoopPolicy(5, 1)
        );

        assertEquals(AgentExecutionStatus.MAX_TOOL_CALLS_REACHED, result.status());
        assertEquals(1, result.iterations());
        assertEquals(0, result.toolCalls());
        assertEquals("needs tools", result.responseText());
    }

    @Test
    void returnsMaxIterationsReachedWhenToolCallsArriveOnFinalAllowedIteration() {
        when(aiToolScopeResolver.resolveTools("admin", Set.of("get_current_time"))).thenReturn(List.of());
        when(aiChatService.chat(any(AiChatRequest.class)))
                .thenReturn(new AiChatResponse(
                        "still working",
                        List.of(new AiToolCall("get_current_time", Map.of())),
                        false
                ));

        AgentExecutionResult result = agentLoopExecutor.execute(
                context(),
                agentConfig(),
                new AgentExecutionRequest(message(), new AiToolScope(Set.of("get_current_time"))),
                new AgentLoopPolicy(1, 5)
        );

        assertEquals(AgentExecutionStatus.MAX_ITERATIONS_REACHED, result.status());
        assertEquals(1, result.iterations());
        assertEquals(0, result.toolCalls());
        assertEquals("still working", result.responseText());
    }

    @Test
    void returnsMaxIterationsReachedImmediatelyWhenPolicyAllowsZeroIterations() {
        AgentExecutionResult result = agentLoopExecutor.execute(
                context(),
                agentConfig(),
                new AgentExecutionRequest(message(), null),
                new AgentLoopPolicy(0, 5)
        );

        assertEquals(AgentExecutionStatus.MAX_ITERATIONS_REACHED, result.status());
        assertEquals(0, result.iterations());
        assertEquals(0, result.toolCalls());
        verify(aiChatService, org.mockito.Mockito.never()).chat(any(AiChatRequest.class));
    }

    @Test
    void returnsRecoverableErrorWhenToolIsNotAllowed() {
        when(aiToolScopeResolver.resolveTools("admin", Set.of("get_current_time"))).thenReturn(List.of());
        when(aiChatService.chat(any(AiChatRequest.class)))
                .thenReturn(new AiChatResponse(
                        "",
                        List.of(new AiToolCall("not_allowed_tool", Map.of("x", 1))),
                        false
                ))
                .thenReturn(new AiChatResponse("handled missing tool", List.of(), true));

        AgentExecutionResult result = agentLoopExecutor.execute(
                context(),
                agentConfig(),
                new AgentExecutionRequest(message(), new AiToolScope(Set.of("get_current_time"))),
                new AgentLoopPolicy(5, 5)
        );

        assertEquals(AgentExecutionStatus.SUCCESS, result.status());
        assertEquals(1, result.toolResults().size());
        assertTrue(String.valueOf(result.toolResults().getFirst().result()).contains("Tool not allowed: not_allowed_tool"));
    }

    @Test
    void skipsToolResolutionWhenToolScopeMissing() {
        when(aiChatService.chat(any(AiChatRequest.class)))
                .thenReturn(new AiChatResponse("done", List.of(), true));

        AgentExecutionResult result = agentLoopExecutor.execute(
                context(),
                agentConfig(),
                new AgentExecutionRequest(message(), null),
                new AgentLoopPolicy(5, 5)
        );

        assertEquals(AgentExecutionStatus.SUCCESS, result.status());
        verify(aiToolScopeResolver, org.mockito.Mockito.never()).resolveTools(any(), any());
    }

    @Test
    void skipsToolResolutionWhenAllowedToolNamesAreNull() {
        when(aiChatService.chat(any(AiChatRequest.class)))
                .thenReturn(new AiChatResponse("done", List.of(), true));

        AgentExecutionResult result = agentLoopExecutor.execute(
                context(),
                agentConfig(),
                new AgentExecutionRequest(message(), new AiToolScope(null)),
                new AgentLoopPolicy(5, 5)
        );

        assertEquals(AgentExecutionStatus.SUCCESS, result.status());
        verify(aiToolScopeResolver, org.mockito.Mockito.never()).resolveTools(any(), any());
    }

    @Test
    void buildsInitialPromptWithBlankSummaryAndPayloadWhenMessageFieldsAreNull() {
        when(aiChatService.chat(any(AiChatRequest.class)))
                .thenReturn(new AiChatResponse("done", List.of(), true));

        agentLoopExecutor.execute(
                context(),
                agentConfig(),
                new AgentExecutionRequest(messageWithNullSummaryAndPayload(), null),
                new AgentLoopPolicy(5, 5)
        );

        ArgumentCaptor<AiChatRequest> requestCaptor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiChatService).chat(requestCaptor.capture());
        String prompt = requestCaptor.getValue().messages().get(1).content();
        assertTrue(prompt.contains("Summary: \n"));
        assertTrue(prompt.contains("Payload:\n"));
    }

    @Test
    void fallsBackToStringValueWhenToolLogSerializationFails() throws Exception {
        AiChatService localChatService = mock(AiChatService.class);
        AiToolScopeResolver localResolver = mock(AiToolScopeResolver.class);
        ObjectMapper localObjectMapper = mock(ObjectMapper.class);
        AgentLoopExecutorImpl localExecutor = new AgentLoopExecutorImpl(localChatService, localResolver, localObjectMapper);

        AiTool tool = mock(AiTool.class);
        when(tool.name()).thenReturn("get_current_time");
        when(tool.execute(any(AiToolExecutionRequest.class))).thenReturn(Map.of("currentTime", "2026-04-05T12:00:00Z"));
        when(localResolver.resolveTools("admin", Set.of("get_current_time"))).thenReturn(List.of(tool));
        when(localChatService.chat(any(AiChatRequest.class)))
                .thenReturn(new AiChatResponse(
                        "",
                        List.of(new AiToolCall("get_current_time", Map.of("timezone", "UTC"))),
                        false
                ))
                .thenReturn(new AiChatResponse("done", List.of(), true));
        doAnswer(invocation -> {
            Object value = invocation.getArgument(0);
            if (value instanceof List<?>) {
                return "[{\"toolName\":\"get_current_time\",\"result\":{\"currentTime\":\"2026-04-05T12:00:00Z\"}}]";
            }
            throw new IllegalStateException("log serialization failed");
        }).when(localObjectMapper).writeValueAsString(any());

        AgentExecutionResult result = localExecutor.execute(
                context(),
                agentConfig(),
                new AgentExecutionRequest(message(), new AiToolScope(Set.of("get_current_time"))),
                new AgentLoopPolicy(5, 5)
        );

        assertEquals(AgentExecutionStatus.SUCCESS, result.status());
        assertEquals(1, result.toolResults().size());
    }

    @Test
    void throwsWhenToolResponseSerializationFails() throws Exception {
        AiChatService localChatService = mock(AiChatService.class);
        AiToolScopeResolver localResolver = mock(AiToolScopeResolver.class);
        ObjectMapper localObjectMapper = mock(ObjectMapper.class);
        AgentLoopExecutorImpl localExecutor = new AgentLoopExecutorImpl(localChatService, localResolver, localObjectMapper);

        AiTool tool = mock(AiTool.class);
        when(tool.name()).thenReturn("get_current_time");
        when(tool.execute(any(AiToolExecutionRequest.class))).thenReturn(Map.of("currentTime", "2026-04-05T12:00:00Z"));
        when(localResolver.resolveTools("admin", Set.of("get_current_time"))).thenReturn(List.of(tool));
        when(localChatService.chat(any(AiChatRequest.class)))
                .thenReturn(new AiChatResponse(
                        "",
                        List.of(new AiToolCall("get_current_time", Map.of("timezone", "UTC"))),
                        false
                ));
        doAnswer(invocation -> {
            Object value = invocation.getArgument(0);
            if (value instanceof List<?>) {
                throw new IllegalStateException("cannot serialize tool response");
            }
            return String.valueOf(value);
        }).when(localObjectMapper).writeValueAsString(any());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> localExecutor.execute(
                        context(),
                        agentConfig(),
                        new AgentExecutionRequest(message(), new AiToolScope(Set.of("get_current_time"))),
                        new AgentLoopPolicy(5, 5)
                )
        );

        assertEquals("Unable to serialize tool responses", exception.getMessage());
    }

    private MessageProcessingContext context() {
        return new MessageProcessingContext(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "admin",
                "run-1",
                new AiMemoryScope("msg:1:run:1")
        );
    }

    private AgentConfig agentConfig() {
        return new AgentConfig(
                new User("admin", "Admin", "admin@example.com", "hash", false),
                "Finance Agent",
                "Handles finance workflows",
                "You are the finance agent.",
                "gemini-2.5-flash",
                0.3
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
                "Please follow up on the invoice",
                "Invoice follow-up",
                Map.of(),
                Map.of(),
                Instant.parse("2026-04-05T12:00:00Z"),
                Instant.parse("2026-04-05T11:55:00Z")
        );
        ReflectionTestUtils.setField(message, "id", UUID.fromString("22222222-2222-2222-2222-222222222222"));
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
        ReflectionTestUtils.setField(message, "id", UUID.fromString("33333333-3333-3333-3333-333333333333"));
        return message;
    }
}
