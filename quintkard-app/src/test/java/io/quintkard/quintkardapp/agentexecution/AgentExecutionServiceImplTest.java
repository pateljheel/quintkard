package io.quintkard.quintkardapp.agentexecution;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quintkard.quintkardapp.agent.AgentConfig;
import io.quintkard.quintkardapp.agenttool.AiTool;
import io.quintkard.quintkardapp.aimodel.AiMemoryScope;
import io.quintkard.quintkardapp.aimodel.AiToolScope;
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

class AgentExecutionServiceImplTest {

    private AgentLoopExecutor agentLoopExecutor;
    private AgentExecutionServiceImpl agentExecutionService;

    @BeforeEach
    void setUp() {
        agentLoopExecutor = mock(AgentLoopExecutor.class);
        AiTool cardTool = mock(AiTool.class);
        AiTool timeTool = mock(AiTool.class);
        when(cardTool.name()).thenReturn("create_card");
        when(timeTool.name()).thenReturn("get_current_time");
        agentExecutionService = new AgentExecutionServiceImpl(
                agentLoopExecutor,
                List.of(cardTool, timeTool),
                new AgentExecutionProperties(7, 9)
        );
    }

    @Test
    void appliesDefaultToolScopeAndConfiguredLoopPolicy() {
        MessageProcessingContext context = new MessageProcessingContext(
                UUID.randomUUID(),
                "admin",
                "run-1",
                new AiMemoryScope("msg:1:run:1")
        );
        AgentConfig agentConfig = new AgentConfig(
                new User("admin", "Admin", "admin@example.com", "hash", false),
                "Finance",
                "Finance agent",
                "Prompt",
                "gemini-2.5-flash",
                0.2
        );
        AgentExecutionRequest request = new AgentExecutionRequest(message(), null);
        AgentExecutionResult expected = new AgentExecutionResult("done", AgentExecutionStatus.SUCCESS, 1, 0, List.of());
        when(agentLoopExecutor.execute(any(), any(), any(), any())).thenReturn(expected);

        AgentExecutionResult result = agentExecutionService.execute(context, agentConfig, request);

        assertSame(expected, result);

        ArgumentCaptor<AgentExecutionRequest> requestCaptor = ArgumentCaptor.forClass(AgentExecutionRequest.class);
        ArgumentCaptor<AgentLoopPolicy> policyCaptor = ArgumentCaptor.forClass(AgentLoopPolicy.class);
        verify(agentLoopExecutor).execute(org.mockito.ArgumentMatchers.eq(context), org.mockito.ArgumentMatchers.eq(agentConfig), requestCaptor.capture(), policyCaptor.capture());
        assertSame(request.message(), requestCaptor.getValue().message());
        org.junit.jupiter.api.Assertions.assertEquals(Set.of("create_card", "get_current_time"), requestCaptor.getValue().toolScope().allowedToolNames());
        org.junit.jupiter.api.Assertions.assertEquals(new AgentLoopPolicy(7, 9), policyCaptor.getValue());
    }

    @Test
    void preservesExplicitToolScope() {
        MessageProcessingContext context = new MessageProcessingContext(UUID.randomUUID(), "admin", "run-2", new AiMemoryScope("scope"));
        AgentConfig agentConfig = new AgentConfig(
                new User("admin", "Admin", "admin@example.com", "hash", false),
                "Finance",
                "Finance agent",
                "Prompt",
                "gemini-2.5-flash",
                0.2
        );
        AgentExecutionRequest request = new AgentExecutionRequest(message(), new AiToolScope(Set.of("create_card")));
        AgentExecutionResult expected = new AgentExecutionResult("done", AgentExecutionStatus.SUCCESS, 1, 0, List.of());
        when(agentLoopExecutor.execute(any(), any(), any(), any())).thenReturn(expected);

        agentExecutionService.execute(context, agentConfig, request);

        ArgumentCaptor<AgentExecutionRequest> requestCaptor = ArgumentCaptor.forClass(AgentExecutionRequest.class);
        verify(agentLoopExecutor).execute(org.mockito.ArgumentMatchers.eq(context), org.mockito.ArgumentMatchers.eq(agentConfig), requestCaptor.capture(), any());
        org.junit.jupiter.api.Assertions.assertEquals(Set.of("create_card"), requestCaptor.getValue().toolScope().allowedToolNames());
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
        ReflectionTestUtils.setField(message, "id", UUID.fromString("33333333-3333-3333-3333-333333333333"));
        return message;
    }
}
