package io.quintkard.quintkardapp.agent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentConfigRepository extends JpaRepository<AgentConfig, UUID> {

    long countByUser_UserId(String userId);

    Optional<AgentConfig> findByIdAndUser_UserId(UUID id, String userId);

    Optional<AgentConfig> findByUser_UserIdAndName(String userId, String name);

    List<AgentConfig> findAllByUser_UserId(String userId);
}
