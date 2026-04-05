package io.quintkard.quintkardapp.agenttool;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class DefaultAiToolRegistry implements AiToolRegistry {

    private final List<AiTool> tools;

    public DefaultAiToolRegistry(List<AiTool> tools) {
        this.tools = tools;
    }

    @Override
    public Optional<AiTool> findTool(String toolName) {
        return tools.stream()
                .filter(tool -> tool.name().equals(toolName))
                .findFirst();
    }
}
