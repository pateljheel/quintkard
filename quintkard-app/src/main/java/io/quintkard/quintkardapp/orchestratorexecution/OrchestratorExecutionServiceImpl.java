package io.quintkard.quintkardapp.orchestratorexecution;

import io.quintkard.quintkardapp.agent.AgentConfig;
import io.quintkard.quintkardapp.agentexecution.AgentExecutionResult;
import io.quintkard.quintkardapp.aimodel.AiMessage;
import io.quintkard.quintkardapp.aimodel.AiMessageRole;
import io.quintkard.quintkardapp.aimodel.AiStructuredChatRequest;
import io.quintkard.quintkardapp.aimodel.AiChatService;
import io.quintkard.quintkardapp.logging.LogContext;
import io.quintkard.quintkardapp.message.Message;
import io.quintkard.quintkardapp.messagepipeline.MessageProcessingContext;
import io.quintkard.quintkardapp.messagepipeline.MessageProcessingContextFactory;
import io.quintkard.quintkardapp.orchestrator.OrchestratorConfig;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorExecutionServiceImpl implements OrchestratorExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(OrchestratorExecutionServiceImpl.class);

    private final AiChatService aiChatService;
    private final AgentDispatchService agentDispatchService;
    private final MessageProcessingContextFactory contextFactory;

    public OrchestratorExecutionServiceImpl(
            AiChatService aiChatService,
            AgentDispatchService agentDispatchService,
            MessageProcessingContextFactory contextFactory
    ) {
        this.aiChatService = aiChatService;
        this.agentDispatchService = agentDispatchService;
        this.contextFactory = contextFactory;
    }

    @Override
    public OrchestratorExecutionResult execute(OrchestratorConfig config, Message message) {
        MessageProcessingContext context = contextFactory.createContext(message);
        try (AutoCloseable ignored = LogContext.with(Map.of("runId", context.runId()))) {
            logger.info("Orchestration started");
            FilteringDecision filteringDecision = runFiltering(config, message, context);
            logger.info("Filtering decision accepted={} reason={}", filteringDecision.accepted(), filteringDecision.reason());
            if (!filteringDecision.accepted()) {
                logger.info("Orchestration completed without routing");
                return new OrchestratorExecutionResult(
                        filteringDecision,
                        new RoutingDecision(List.of(), "Message rejected by filtering step"),
                        Map.of()
                );
            }

            RoutingDecision routingDecision = runRouting(config, message, context);
            logger.info("Routing decision agentCount={} reason={}", routingDecision.agentIds().size(), routingDecision.reason());
            Map<UUID, AgentExecutionResult> agentResults =
                    agentDispatchService.dispatch(config, routingDecision, context, message);
            logger.info("Orchestration completed dispatchedAgents={}", agentResults.size());
            return new OrchestratorExecutionResult(filteringDecision, routingDecision, agentResults);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to manage orchestration logging context", exception);
        }
    }

    private FilteringDecision runFiltering(
            OrchestratorConfig config,
            Message message,
            MessageProcessingContext context
    ) {
        if (config.getFilteringPrompt() == null || config.getFilteringPrompt().isBlank()) {
            return new FilteringDecision(true, "Filtering disabled");
        }

        return aiChatService.chatForObject(new AiStructuredChatRequest<>(
                message.getUser().getUserId(),
                config.getFilteringModel(),
                0.1,
                List.of(
                        new AiMessage(AiMessageRole.SYSTEM, config.getFilteringPrompt()),
                        new AiMessage(AiMessageRole.USER, buildMessageContext(message))
                ),
                context.memoryScope(),
                null,
                FilteringDecision.class
        ));
    }

    private RoutingDecision runRouting(
            OrchestratorConfig config,
            Message message,
            MessageProcessingContext context
    ) {
        if (config.getActiveAgents().isEmpty()) {
            return new RoutingDecision(List.of(), "No active agents configured");
        }

        RoutingDecision rawDecision = aiChatService.chatForObject(new AiStructuredChatRequest<>(
                message.getUser().getUserId(),
                config.getRoutingModel(),
                0.1,
                List.of(
                        new AiMessage(AiMessageRole.SYSTEM, config.getRoutingPrompt()),
                        new AiMessage(AiMessageRole.USER, buildRoutingContext(message, config.getActiveAgents()))
                ),
                context.memoryScope(),
                null,
                RoutingDecision.class
        ));

        Set<UUID> allowedAgentIds = config.getActiveAgents().stream()
                .map(AgentConfig::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<UUID> selectedAgentIds = rawDecision.agentIds() == null
                ? List.of()
                : rawDecision.agentIds().stream()
                        .filter(allowedAgentIds::contains)
                        .distinct()
                        .toList();

        String reason = rawDecision.reason() == null || rawDecision.reason().isBlank()
                ? "No routing reason provided"
                : rawDecision.reason();

        return new RoutingDecision(selectedAgentIds, reason);
    }

    private String buildMessageContext(Message message) {
        return """
                Message ID: %s
                Source service: %s
                Message type: %s
                Summary: %s
                Payload:
                %s
                """.formatted(
                message.getId(),
                message.getSourceService(),
                message.getMessageType(),
                nullToEmpty(message.getSummary()),
                nullToEmpty(message.getPayload())
        );
    }

    private String buildRoutingContext(Message message, Set<AgentConfig> activeAgents) {
        String availableAgents = activeAgents.stream()
                .map(agent -> """
                        Agent ID: %s
                        Name: %s
                        Description: %s
                        """.formatted(agent.getId(), agent.getName(), agent.getDescription()))
                .collect(Collectors.joining("\n"));

        return """
                Route this message only to the agent IDs listed below.

                Available agents:
                %s

                Message context:
                %s
                """.formatted(availableAgents, buildMessageContext(message));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
