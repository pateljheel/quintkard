package io.quintkard.quintkardapp.orchestratorexecution;

import io.quintkard.quintkardapp.agent.AgentConfig;
import io.quintkard.quintkardapp.agentexecution.AgentExecutionRequest;
import io.quintkard.quintkardapp.agentexecution.AgentExecutionResult;
import io.quintkard.quintkardapp.agentexecution.AgentExecutionService;
import io.quintkard.quintkardapp.logging.LogContext;
import io.quintkard.quintkardapp.message.Message;
import io.quintkard.quintkardapp.messagepipeline.MessageProcessingContext;
import io.quintkard.quintkardapp.orchestrator.OrchestratorConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AgentDispatchServiceImpl implements AgentDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(AgentDispatchServiceImpl.class);

    private final AgentExecutionService agentExecutionService;

    public AgentDispatchServiceImpl(AgentExecutionService agentExecutionService) {
        this.agentExecutionService = agentExecutionService;
    }

    @Override
    public Map<UUID, AgentExecutionResult> dispatch(
            OrchestratorConfig config,
            RoutingDecision routingDecision,
            MessageProcessingContext context,
            Message message
    ) {
        Map<UUID, AgentExecutionResult> results = new LinkedHashMap<>();
        if (routingDecision.agentIds() == null || routingDecision.agentIds().isEmpty()) {
            return results;
        }

        Map<UUID, AgentConfig> activeAgentsById = new LinkedHashMap<>();
        for (AgentConfig activeAgent : config.getActiveAgents()) {
            activeAgentsById.put(activeAgent.getId(), activeAgent);
        }

        for (UUID agentId : routingDecision.agentIds()) {
            AgentConfig agentConfig = activeAgentsById.get(agentId);
            if (agentConfig == null) {
                continue;
            }

            try (AutoCloseable ignored = LogContext.with(Map.of("agentId", agentId.toString()))) {
                logger.info("Agent dispatch started agentName={}", agentConfig.getName());
                AgentExecutionResult result = agentExecutionService.execute(
                        context,
                        agentConfig,
                        new AgentExecutionRequest(message, null)
                );
                results.put(agentId, result);
                logger.info("Agent dispatch completed status={} iterations={} toolCalls={}",
                        result.status(), result.iterations(), result.toolCalls());
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to manage agent logging context", exception);
            }
        }

        return results;
    }
}
