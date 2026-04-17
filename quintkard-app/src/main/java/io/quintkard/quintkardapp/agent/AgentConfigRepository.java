package io.quintkard.quintkardapp.agent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface AgentConfigRepository extends Repository<AgentConfig, UUID> {

    AgentConfig save(AgentConfig agentConfig);

    long countByUser_UserId(String userId);

    Optional<AgentConfig> findByIdAndUser_UserId(UUID id, String userId);

    Optional<AgentConfig> findByUser_UserIdAndName(String userId, String name);

    List<AgentConfig> findAllByUser_UserId(String userId);

    void delete(AgentConfig agentConfig);
}
