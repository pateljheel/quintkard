package io.quintkard.quintkardapp.agent;

import java.util.List;
import java.util.UUID;

public interface AgentService {

    AgentConfig createAgent(String userId, AgentConfigRequest request);

    AgentConfig updateAgent(String userId, UUID agentId, AgentConfigRequest request);

    AgentConfig getAgent(String userId, UUID agentId);

    List<AgentConfig> listAgents(String userId);

    void deleteAgent(String userId, UUID agentId);
}
