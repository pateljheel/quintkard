package io.quintkard.quintkardapp.agenttool;

import java.util.Optional;

public interface AiToolRegistry {

    Optional<AiTool> findTool(String toolName);
}
