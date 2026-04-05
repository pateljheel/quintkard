package io.quintkard.quintkardapp.agenttool;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DefaultAiToolScopeResolver implements AiToolScopeResolver {

    private final AiToolRegistry aiToolRegistry;

    public DefaultAiToolScopeResolver(AiToolRegistry aiToolRegistry) {
        this.aiToolRegistry = aiToolRegistry;
    }

    @Override
    public List<AiTool> resolveTools(String userId, Set<String> allowedToolNames) {
        List<AiTool> tools = new ArrayList<>();
        for (String toolName : allowedToolNames) {
            aiToolRegistry.findTool(toolName).ifPresent(tools::add);
        }
        return tools;
    }
}
