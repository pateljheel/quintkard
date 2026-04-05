package io.quintkard.quintkardapp.aimodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quintkard.quintkardapp.agenttool.AiTool;
import io.quintkard.quintkardapp.agenttool.AiToolExecutionRequest;
import io.quintkard.quintkardapp.agenttool.AiToolScopeResolver;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import tools.jackson.databind.ObjectMapper;

class SpringAiChatServiceTest {

    private ChatMemory chatMemory;
    private ChatModel chatModel;
    private AiToolScopeResolver toolScopeResolver;
    private SpringAiChatService service;

    @BeforeEach
    void setUp() {
        chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(100)
                .build();
        chatModel = mock(ChatModel.class);
        toolScopeResolver = mock(AiToolScopeResolver.class);
        service = new SpringAiChatService(chatMemory, chatModel, toolScopeResolver, new ObjectMapper());
    }

    @Test
    void chatUsesExistingMemoryPersistsNonSystemMessagesAndAssistantReply() {
        AiMemoryScope memoryScope = new AiMemoryScope("conv-1");
        chatMemory.add(memoryScope.conversationId(), new UserMessage("previous user message"));
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("done")))));

        AiChatResponse response = service.chat(new AiChatRequest(
                "admin",
                "gemini-2.5-flash",
                0.2,
                List.of(
                        new AiMessage(AiMessageRole.SYSTEM, "system prompt"),
                        new AiMessage(AiMessageRole.USER, "new user message")
                ),
                memoryScope,
                null
        ));

        assertEquals("done", response.text());
        assertTrue(response.finalResponse());
        assertEquals(List.of(), response.toolCalls());

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        Prompt prompt = promptCaptor.getValue();
        assertEquals(3, prompt.getInstructions().size());
        assertEquals("previous user message", prompt.getInstructions().get(0).getText());
        assertEquals("system prompt", prompt.getInstructions().get(1).getText());
        assertEquals("new user message", prompt.getInstructions().get(2).getText());

        List<Message> memoryMessages = chatMemory.get(memoryScope.conversationId());
        assertEquals(3, memoryMessages.size());
        assertInstanceOf(UserMessage.class, memoryMessages.get(0));
        assertEquals("previous user message", memoryMessages.get(0).getText());
        assertInstanceOf(UserMessage.class, memoryMessages.get(1));
        assertEquals("new user message", memoryMessages.get(1).getText());
        assertInstanceOf(AssistantMessage.class, memoryMessages.get(2));
        assertEquals("done", memoryMessages.get(2).getText());
        assertFalse(memoryMessages.stream().anyMatch(SystemMessage.class::isInstance));
    }

    @Test
    void chatExposesToolCallsAndBuildsToolOptions() {
        AiTool tool = mock(AiTool.class);
        when(tool.name()).thenReturn("get_current_time");
        when(tool.description()).thenReturn("Gets current time");
        doReturn(Map.class).when(tool).inputType();
        when(toolScopeResolver.resolveTools("admin", Set.of("get_current_time"))).thenReturn(List.of(tool));
        AssistantMessage assistantMessage = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("1", "function", "get_current_time", "{\"timeZone\":\"UTC\"}")))
                .build();
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(assistantMessage))));

        AiChatResponse response = service.chat(new AiChatRequest(
                "admin",
                "gemini-2.5-flash",
                0.3,
                List.of(new AiMessage(AiMessageRole.USER, "what time is it?")),
                null,
                new AiToolScope(Set.of("get_current_time"))
        ));

        assertFalse(response.finalResponse());
        assertEquals(1, response.toolCalls().size());
        assertEquals("get_current_time", response.toolCalls().getFirst().toolName());
        assertEquals("UTC", response.toolCalls().getFirst().arguments().get("timeZone"));

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        GoogleGenAiChatOptions options = (GoogleGenAiChatOptions) promptCaptor.getValue().getOptions();
        assertEquals("gemini-2.5-flash", options.getModel());
        assertEquals(0.3, options.getTemperature());
        assertEquals(Set.of("get_current_time"), options.getToolNames());
        assertEquals(false, options.getInternalToolExecutionEnabled());
        assertEquals("admin", options.getLabels().get("userId"));
        assertEquals("admin", options.getToolContext().get("userId"));
        assertEquals(1, options.getToolCallbacks().size());
        assertEquals("get_current_time", options.getToolCallbacks().getFirst().getToolDefinition().name());
    }

    @Test
    void chatRejectsInvalidToolArgumentsJson() {
        AssistantMessage assistantMessage = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("1", "function", "bad_tool", "not-json")))
                .build();
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(assistantMessage))));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.chat(new AiChatRequest(
                        "admin",
                        "gemini-2.5-flash",
                        0.3,
                        List.of(new AiMessage(AiMessageRole.USER, "run bad tool")),
                        null,
                        null
                ))
        );

        assertEquals("Unable to parse tool arguments", exception.getMessage());
    }

    @Test
    void chatForObjectMergesFormatInstructionsIntoExistingSystemPrompt() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("{\"accepted\":true,\"reason\":\"ok\"}")))));

        RoutingLikeResponse response = service.chatForObject(new AiStructuredChatRequest<>(
                "admin",
                "gemini-2.5-flash",
                0.1,
                List.of(
                        new AiMessage(AiMessageRole.SYSTEM, "Route carefully"),
                        new AiMessage(AiMessageRole.USER, "Decide")
                ),
                null,
                null,
                RoutingLikeResponse.class
        ));

        assertEquals(true, response.accepted());
        assertEquals("ok", response.reason());

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        Prompt prompt = promptCaptor.getValue();
        assertEquals(2, prompt.getInstructions().size());
        assertInstanceOf(SystemMessage.class, prompt.getInstructions().getFirst());
        assertTrue(prompt.getInstructions().getFirst().getText().startsWith("Route carefully"));
        assertTrue(prompt.getInstructions().getFirst().getText().contains("JSON Schema"));
        GoogleGenAiChatOptions options = (GoogleGenAiChatOptions) prompt.getOptions();
        assertNotNull(options.getResponseSchema());
        assertEquals("application/json", options.getResponseMimeType());
    }

    @Test
    void chatForObjectPrependsSystemInstructionsWhenMissing() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("{\"accepted\":false,\"reason\":\"no\"}")))));

        service.chatForObject(new AiStructuredChatRequest<>(
                "admin",
                "gemini-2.5-flash",
                0.1,
                List.of(new AiMessage(AiMessageRole.USER, "Decide")),
                null,
                null,
                RoutingLikeResponse.class
        ));

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        Prompt prompt = promptCaptor.getValue();
        assertInstanceOf(SystemMessage.class, prompt.getInstructions().getFirst());
        assertInstanceOf(UserMessage.class, prompt.getInstructions().get(1));
        assertTrue(prompt.getInstructions().getFirst().getText().contains("JSON Schema"));
    }

    @Test
    void chatConvertsToolResponsePayloadListIntoSingleToolResponseMessage() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("done")))));

        service.chat(new AiChatRequest(
                "admin",
                "gemini-2.5-flash",
                0.2,
                List.of(new AiMessage(
                        AiMessageRole.TOOL,
                        """
                        [{"toolName":"tool_a","result":{"ok":true}},{"toolName":"tool_b","result":"text"}]
                        """
                )),
                null,
                null
        ));

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        Message toolMessage = promptCaptor.getValue().getInstructions().getFirst();
        ToolResponseMessage responseMessage = (ToolResponseMessage) toolMessage;
        assertEquals(2, responseMessage.getResponses().size());
        assertEquals("tool_a", responseMessage.getResponses().get(0).name());
        assertEquals("{\"ok\":true}", responseMessage.getResponses().get(0).responseData());
        assertEquals("\"text\"", responseMessage.getResponses().get(1).responseData());
    }

    @Test
    void chatFallsBackToSingleMapAndRawToolContent() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("done")))));

        service.chat(new AiChatRequest(
                "admin",
                "gemini-2.5-flash",
                0.2,
                List.of(new AiMessage(AiMessageRole.TOOL, "{\"toolName\":\"tool_a\",\"result\":{\"ok\":true}}")),
                null,
                null
        ));

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        ToolResponseMessage mapResponse = (ToolResponseMessage) promptCaptor.getValue().getInstructions().getFirst();
        assertEquals(1, mapResponse.getResponses().size());
        assertEquals("tool_a", mapResponse.getResponses().getFirst().name());

        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("done")))));

        service.chat(new AiChatRequest(
                "admin",
                "gemini-2.5-flash",
                0.2,
                List.of(new AiMessage(AiMessageRole.TOOL, "unstructured tool output")),
                null,
                null
        ));

        ArgumentCaptor<Prompt> secondCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, org.mockito.Mockito.times(2)).call(secondCaptor.capture());
        ToolResponseMessage rawResponse =
                (ToolResponseMessage) secondCaptor.getAllValues().getLast().getInstructions().getFirst();
        assertEquals("tool", rawResponse.getResponses().getFirst().name());
        assertEquals("unstructured tool output", rawResponse.getResponses().getFirst().responseData());
    }

    @Test
    void toolCallbackExecutesThroughResolver() {
        AiTool tool = mock(AiTool.class);
        when(tool.name()).thenReturn("get_current_time");
        when(tool.description()).thenReturn("Gets current time");
        doReturn(Map.class).when(tool).inputType();
        when(tool.execute(any(AiToolExecutionRequest.class))).thenReturn(Map.of("timeZone", "UTC"));
        when(toolScopeResolver.resolveTools("admin", Set.of("get_current_time"))).thenReturn(List.of(tool));
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("done")))));

        service.chat(new AiChatRequest(
                "admin",
                "gemini-2.5-flash",
                0.2,
                List.of(new AiMessage(AiMessageRole.USER, "time")),
                null,
                new AiToolScope(Set.of("get_current_time"))
        ));

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        GoogleGenAiChatOptions options = (GoogleGenAiChatOptions) promptCaptor.getValue().getOptions();
        String callbackResult = options.getToolCallbacks().getFirst().call("{\"timeZone\":\"UTC\"}");
        assertTrue(callbackResult.contains("\"timeZone\":\"UTC\""));
        verify(tool).execute(eq(new AiToolExecutionRequest("admin", null, Map.of("timeZone", "UTC"))));
    }

    private record RoutingLikeResponse(boolean accepted, String reason) {
    }
}
