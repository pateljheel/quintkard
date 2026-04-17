package io.quintkard.quintkardapp.orchestrator;

import io.quintkard.quintkardapp.agent.AgentConfig;
import io.quintkard.quintkardapp.agent.AgentConfigRepository;
import io.quintkard.quintkardapp.aimodel.AiModelCatalog;
import io.quintkard.quintkardapp.user.User;
import io.quintkard.quintkardapp.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class OrchestratorServiceImpl implements OrchestratorService {

    private final AgentConfigRepository agentRepository;
    private final AiModelCatalog modelCatalog;
    private final OrchestratorConfigRepository orchestratorConfigRepository;
    private final UserRepository userRepository;

    public OrchestratorServiceImpl(
        AgentConfigRepository agentRepository,
        AiModelCatalog modelCatalog,
        OrchestratorConfigRepository orchestratorConfigRepository,
        UserRepository userRepository
    ) {
        this.agentRepository = agentRepository;
        this.modelCatalog = modelCatalog;
        this.orchestratorConfigRepository = orchestratorConfigRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OrchestratorConfig getConfig(String userId) {
        return orchestratorConfigRepository.findByUser_UserId(userId)
            .orElseGet(() -> new OrchestratorConfig(getUser(userId), "", "", "", "", Set.of()));
    }

    @Override
    @Transactional
    public OrchestratorConfig updateConfig(String userId, OrchestratorConfigRequest request) {
        validateRequest(request);

        Set<AgentConfig> activeAgents = resolveActiveAgents(userId, request.activeAgentIds());
        OrchestratorConfig config = orchestratorConfigRepository.findByUser_UserId(userId)
            .orElseGet(() -> new OrchestratorConfig(getUser(userId), "", "", "", "", Set.of()));

        config.update(
            trimToEmpty(request.filteringPrompt()),
            trimToEmpty(request.filteringModel()),
            request.routingPrompt().trim(),
            request.routingModel().trim(),
            activeAgents
        );

        return orchestratorConfigRepository.save(config);
    }

    private void validateRequest(OrchestratorConfigRequest request) {
        if (request.routingPrompt() == null || request.routingPrompt().isBlank()) {
            throw new IllegalArgumentException("Routing prompt is required");
        }
        if (request.routingModel() == null || request.routingModel().isBlank()) {
            throw new IllegalArgumentException("Routing model is required");
        }
        modelCatalog.getModel(request.routingModel().trim());
        if (request.activeAgentIds() == null) {
            throw new IllegalArgumentException("Active agent IDs are required");
        }

        if (request.filteringPrompt() != null && !request.filteringPrompt().isBlank()) {
            if (request.filteringModel() == null || request.filteringModel().isBlank()) {
                throw new IllegalArgumentException("Filtering model is required when filtering prompt is provided");
            }
            modelCatalog.getModel(request.filteringModel().trim());
        }
    }

    private Set<AgentConfig> resolveActiveAgents(String userId, List<UUID> agentIds) {
        Set<AgentConfig> activeAgents = new LinkedHashSet<>();
        for (UUID agentId : agentIds) {
            AgentConfig agent = agentRepository.findByIdAndUser_UserId(agentId, userId)
                .orElseThrow(() -> new NoSuchElementException(
                    "Agent not found for user %s: %s".formatted(userId, agentId)
                ));
            activeAgents.add(agent);
        }
        return activeAgents;
    }

    private User getUser(String userId) {
        return userRepository.findByUserId(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
