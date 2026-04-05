package io.quintkard.quintkardapp.agenttool;

import java.util.List;
import java.util.Set;

public interface AiToolScopeResolver {

    List<AiTool> resolveTools(String userId, Set<String> allowedToolNames);
}
