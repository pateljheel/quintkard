package io.quintkard.quintkardapp.agentexecution;

import io.quintkard.quintkardapp.agent.AgentConfig;
import io.quintkard.quintkardapp.agenttool.AiTool;
import io.quintkard.quintkardapp.aimodel.AiToolScope;
import io.quintkard.quintkardapp.messagepipeline.MessageProcessingContext;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AgentExecutionServiceImpl implements AgentExecutionService {

    private final AgentLoopExecutor agentLoopExecutor;
    private final List<AiTool> aiTools;
    private final AgentExecutionProperties agentExecutionProperties;

    public AgentExecutionServiceImpl(
            AgentLoopExecutor agentLoopExecutor,
            List<AiTool> aiTools,
            AgentExecutionProperties agentExecutionProperties
    ) {
        this.agentLoopExecutor = agentLoopExecutor;
        this.aiTools = aiTools;
        this.agentExecutionProperties = agentExecutionProperties;
    }

    @Override
    public AgentExecutionResult execute(
            MessageProcessingContext context,
            AgentConfig agentConfig,
            AgentExecutionRequest request
    ) {
        AgentExecutionRequest normalizedRequest = new AgentExecutionRequest(
                request.message(),
                request.toolScope() == null
                        ? new AiToolScope(defaultToolNames())
                        : request.toolScope()
        );

        return agentLoopExecutor.execute(
                context,
                agentConfig,
                normalizedRequest,
                new AgentLoopPolicy(
                        agentExecutionProperties.maxIterations(),
                        agentExecutionProperties.maxToolCalls()
                )
        );
    }

    private Set<String> defaultToolNames() {
        Set<String> names = new LinkedHashSet<>();
        for (AiTool aiTool : aiTools) {
            names.add(aiTool.name());
        }
        return names;
    }
}
