package io.quintkard.quintkardapp.orchestrator;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrchestratorConfigRepository extends JpaRepository<OrchestratorConfig, UUID> {

    Optional<OrchestratorConfig> findByUser_UserId(String userId);
}
