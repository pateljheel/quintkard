package io.quintkard.quintkardapp.agenttool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiToolRegistryTest {

    @Test
    void defaultRegistryFindsToolByName() {
        AiTool cardTool = mock(AiTool.class);
        AiTool timeTool = mock(AiTool.class);
        when(cardTool.name()).thenReturn("create_card");
        when(timeTool.name()).thenReturn("get_current_time");

        DefaultAiToolRegistry registry = new DefaultAiToolRegistry(List.of(cardTool, timeTool));

        assertSame(cardTool, registry.findTool("create_card").orElseThrow());
        assertTrue(registry.findTool("missing").isEmpty());
    }

    @Test
    void defaultScopeResolverReturnsOnlyAllowedRegisteredToolsInRequestedOrder() {
        AiToolRegistry registry = mock(AiToolRegistry.class);
        AiTool createTool = mock(AiTool.class);
        AiTool timeTool = mock(AiTool.class);
        when(registry.findTool("create_card")).thenReturn(java.util.Optional.of(createTool));
        when(registry.findTool("missing")).thenReturn(java.util.Optional.empty());
        when(registry.findTool("get_current_time")).thenReturn(java.util.Optional.of(timeTool));

        DefaultAiToolScopeResolver resolver = new DefaultAiToolScopeResolver(registry);
        LinkedHashSet<String> allowedToolNames = new LinkedHashSet<>();
        allowedToolNames.add("create_card");
        allowedToolNames.add("missing");
        allowedToolNames.add("get_current_time");

        List<AiTool> result = resolver.resolveTools("admin", allowedToolNames);

        assertEquals(List.of(createTool, timeTool), result);
    }
}
