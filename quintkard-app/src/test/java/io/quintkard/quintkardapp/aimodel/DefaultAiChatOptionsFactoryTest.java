package io.quintkard.quintkardapp.aimodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;

class DefaultAiChatOptionsFactoryTest {

    private AiToolScopeResolver toolScopeResolver;
    private DefaultAiChatOptionsFactory factory;

    @BeforeEach
    void setUp() {
        toolScopeResolver = mock(AiToolScopeResolver.class);
        factory = new DefaultAiChatOptionsFactory(toolScopeResolver);
    }

    @Test
    void buildsGoogleOptionsWithStructuredOutputAndTools() {
        AiTool tool = mock(AiTool.class);
        when(tool.name()).thenReturn("get_current_time");
        when(tool.description()).thenReturn("Gets current time");
        doReturn(Map.class).when(tool).inputType();
        when(tool.execute(new AiToolExecutionRequest("admin", null, Map.of("timeZone", "UTC"))))
                .thenReturn(Map.of("timeZone", "UTC"));
        when(toolScopeResolver.resolveTools("admin", Set.of("get_current_time"))).thenReturn(List.of(tool));

        GoogleGenAiChatOptions options = (GoogleGenAiChatOptions) factory.build(
                AiProvider.GOOGLE_GENAI,
                "admin",
                "gemini-2.5-flash",
                0.3,
                new AiToolScope(Set.of("get_current_time")),
                "{\"type\":\"object\"}",
                "application/json"
        );

        assertEquals("gemini-2.5-flash", options.getModel());
        assertEquals(0.3, options.getTemperature());
        assertEquals(false, options.getInternalToolExecutionEnabled());
        assertEquals("{\"type\":\"object\"}", options.getResponseSchema());
        assertEquals("application/json", options.getResponseMimeType());
        assertEquals("admin", options.getLabels().get("userId"));
        assertEquals(Set.of("get_current_time"), options.getToolNames());
        assertEquals("admin", options.getToolContext().get("userId"));
        assertEquals(1, options.getToolCallbacks().size());
        assertEquals(
                "{\"timeZone\":\"UTC\"}",
                options.getToolCallbacks().getFirst().call("{\"timeZone\":\"UTC\"}")
        );
        verify(tool).execute(new AiToolExecutionRequest("admin", null, Map.of("timeZone", "UTC")));
    }

    @Test
    void buildsOpenAiOptionsWithStructuredOutputAndTools() {
        AiTool tool = mock(AiTool.class);
        when(tool.name()).thenReturn("get_current_time");
        when(tool.description()).thenReturn("Gets current time");
        doReturn(Map.class).when(tool).inputType();
        when(tool.execute(new AiToolExecutionRequest("admin", null, Map.of("timeZone", "UTC"))))
                .thenReturn(Map.of("timeZone", "UTC"));
        when(toolScopeResolver.resolveTools("admin", Set.of("get_current_time"))).thenReturn(List.of(tool));

        OpenAiChatOptions options = (OpenAiChatOptions) factory.build(
                AiProvider.OPENAI,
                "admin",
                "gpt-5.4-mini",
                0.4,
                new AiToolScope(Set.of("get_current_time")),
                "{\"type\":\"object\"}",
                "application/json"
        );

        assertEquals("gpt-5.4-mini", options.getModel());
        assertEquals(0.4, options.getTemperature());
        assertEquals(false, options.getInternalToolExecutionEnabled());
        assertEquals("{\"type\":\"object\"}", options.getOutputSchema());
        assertEquals("admin", options.getUser());
        assertNull(options.getMetadata());
        assertEquals(Set.of("get_current_time"), options.getToolNames());
        assertEquals("admin", options.getToolContext().get("userId"));
        assertEquals(1, options.getToolCallbacks().size());
        assertEquals(
                "{\"timeZone\":\"UTC\"}",
                options.getToolCallbacks().getFirst().call("{\"timeZone\":\"UTC\"}")
        );
        verify(tool).execute(new AiToolExecutionRequest("admin", null, Map.of("timeZone", "UTC")));
    }

    @Test
    void skipsToolResolutionWhenToolScopeMissing() {
        OpenAiChatOptions options = (OpenAiChatOptions) factory.build(
                AiProvider.OPENAI,
                "admin",
                "gpt-5.4-mini",
                0.2,
                null,
                null,
                null
        );

        assertEquals(Set.of(), options.getToolNames());
        assertEquals(List.of(), options.getToolCallbacks());
        assertEquals(Map.of(), options.getToolContext());
    }

    @Test
    void skipsToolResolutionWhenAllowedToolNamesAreNull() {
        GoogleGenAiChatOptions options = (GoogleGenAiChatOptions) factory.build(
                AiProvider.GOOGLE_GENAI,
                "admin",
                "gemini-2.5-flash",
                0.2,
                new AiToolScope(null),
                null,
                null
        );

        assertEquals(Map.of("userId", "admin"), options.getLabels());
        assertEquals(Set.of(), options.getToolNames());
        assertEquals(List.of(), options.getToolCallbacks());
        assertEquals(Map.of(), options.getToolContext());
    }

    @Test
    void skipsToolResolutionWhenAllowedToolNamesAreEmpty() {
        OpenAiChatOptions options = (OpenAiChatOptions) factory.build(
                AiProvider.OPENAI,
                "admin",
                "gpt-5.4-mini",
                0.2,
                new AiToolScope(Set.of()),
                null,
                null
        );

        assertEquals(Set.of(), options.getToolNames());
        assertEquals(List.of(), options.getToolCallbacks());
        assertEquals(Map.of(), options.getToolContext());
        assertNull(options.getOutputSchema());
    }

    @Test
    void buildsGoogleOptionsWithoutStructuredOutputWhenSchemaAndMimeTypeAreAbsent() {
        GoogleGenAiChatOptions options = (GoogleGenAiChatOptions) factory.build(
                AiProvider.GOOGLE_GENAI,
                "admin",
                "gemini-2.5-flash",
                0.2,
                null,
                null,
                null
        );

        assertEquals("gemini-2.5-flash", options.getModel());
        assertEquals(0.2, options.getTemperature());
        assertEquals(false, options.getInternalToolExecutionEnabled());
        assertEquals(Map.of("userId", "admin"), options.getLabels());
        assertNull(options.getResponseSchema());
        assertNull(options.getResponseMimeType());
    }
}
