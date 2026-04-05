package io.quintkard.quintkardapp.messagepipeline;

import io.quintkard.quintkardapp.message.Message;
import io.quintkard.quintkardapp.orchestrator.OrchestratorConfig;
import io.quintkard.quintkardapp.orchestrator.OrchestratorService;
import io.quintkard.quintkardapp.orchestratorexecution.OrchestratorExecutionResult;
import io.quintkard.quintkardapp.orchestratorexecution.OrchestratorExecutionService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class DefaultMessageProcessor implements MessageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMessageProcessor.class);
    private final OrchestratorService orchestratorService;
    private final OrchestratorExecutionService orchestratorExecutionService;
    private final ObjectMapper objectMapper;

    public DefaultMessageProcessor(
            OrchestratorService orchestratorService,
            OrchestratorExecutionService orchestratorExecutionService,
            ObjectMapper objectMapper
    ) {
        this.orchestratorService = orchestratorService;
        this.orchestratorExecutionService = orchestratorExecutionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(Message message) {
        OrchestratorConfig config = orchestratorService.getConfig(message.getUser().getUserId());
        if (config.getRoutingPrompt() == null || config.getRoutingPrompt().isBlank()
                || config.getRoutingModel() == null || config.getRoutingModel().isBlank()) {
            logger.info("Skipping orchestration for message {} because no routing config is defined", message.getId());
            message.updateDetails(Map.of(
                    "orchestration",
                    Map.of(
                            "status", "SKIPPED",
                            "reason", "No routing configuration defined"
                    )
            ));
            return;
        }

        OrchestratorExecutionResult orchestration = orchestratorExecutionService.execute(config, message);
        message.updateDetails(buildProcessingDetails(orchestration));
        logger.info(
                "Processed message {} through orchestration. Accepted={}, routedAgents={}",
                message.getId(),
                orchestration.filteringDecision().accepted(),
                orchestration.routingDecision().agentIds().size()
        );
    }

    private Map<String, Object> buildProcessingDetails(OrchestratorExecutionResult orchestration) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("orchestration", objectMapper.convertValue(orchestration, Map.class));
        return details;
    }
}
