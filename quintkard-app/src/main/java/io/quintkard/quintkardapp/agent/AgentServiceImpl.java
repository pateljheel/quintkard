package io.quintkard.quintkardapp.agent;

import io.quintkard.quintkardapp.aimodel.AiModelCatalog;
import io.quintkard.quintkardapp.aimodel.AiModelDefinition;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import io.quintkard.quintkardapp.user.User;
import io.quintkard.quintkardapp.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentServiceImpl implements AgentService {

    private static final long MAX_AGENTS_PER_USER = 30;

    private final AgentConfigRepository agentConfigRepository;
    private final AiModelCatalog modelCatalog;
    private final UserRepository userRepository;

    public AgentServiceImpl(
            AgentConfigRepository agentConfigRepository,
            AiModelCatalog modelCatalog,
            UserRepository userRepository
    ) {
        this.agentConfigRepository = agentConfigRepository;
        this.modelCatalog = modelCatalog;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public AgentConfig createAgent(String userId, AgentConfigRequest request) {
        validateRequest(request);
        User user = getUser(userId);

        if (agentConfigRepository.countByUser_UserId(userId) >= MAX_AGENTS_PER_USER) {
            throw new IllegalArgumentException(
                    "Agent limit reached. A user can only have %d agents.".formatted(MAX_AGENTS_PER_USER)
            );
        }

        if (agentConfigRepository.findByUser_UserIdAndName(userId, request.name().trim()).isPresent()) {
            throw new IllegalArgumentException("Agent name already exists: " + request.name());
        }

        AgentConfig agent = new AgentConfig(
                user,
                request.name().trim(),
                request.description().trim(),
                request.prompt().trim(),
                request.model().trim(),
                request.temperature()
        );

        return agentConfigRepository.save(agent);
    }

    @Override
    @Transactional
    public AgentConfig updateAgent(String userId, UUID agentId, AgentConfigRequest request) {
        validateRequest(request);

        AgentConfig agent = getAgent(userId, agentId);
        agentConfigRepository.findByUser_UserIdAndName(userId, request.name().trim())
                .filter(existingAgent -> !existingAgent.getId().equals(agentId))
                .ifPresent(existingAgent -> {
                    throw new IllegalArgumentException("Agent name already exists: " + request.name());
                });

        agent.updateDefinition(
                request.name().trim(),
                request.description().trim(),
                request.prompt().trim(),
                request.model().trim(),
                request.temperature()
        );

        return agent;
    }

    @Override
    @Transactional(readOnly = true)
    public AgentConfig getAgent(String userId, UUID agentId) {
        return agentConfigRepository.findByIdAndUser_UserId(agentId, userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Agent not found for user %s: %s".formatted(userId, agentId)
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentConfig> listAgents(String userId) {
        return agentConfigRepository.findAllByUser_UserId(userId);
    }

    @Override
    @Transactional
    public void deleteAgent(String userId, UUID agentId) {
        agentConfigRepository.delete(getAgent(userId, agentId));
    }

    private void validateRequest(AgentConfigRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Agent name is required");
        }
        if (request.description() == null || request.description().isBlank()) {
            throw new IllegalArgumentException("Agent description is required");
        }
        if (request.prompt() == null || request.prompt().isBlank()) {
            throw new IllegalArgumentException("Agent prompt is required");
        }
        if (request.model() == null || request.model().isBlank()) {
            throw new IllegalArgumentException("Agent model is required");
        }

        AiModelDefinition modelConfig = modelCatalog.getModel(request.model().trim());
        if (request.temperature() < modelConfig.minTemperature()
                || request.temperature() > modelConfig.maxTemperature()) {
            throw new IllegalArgumentException(
                    "Temperature %.2f is invalid for model %s. Supported range: %.1f to %.1f"
                            .formatted(
                                    request.temperature(),
                                    modelConfig.id(),
                                    modelConfig.minTemperature(),
                                    modelConfig.maxTemperature()
                            )
            );
        }
    }

    private User getUser(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    }
}
