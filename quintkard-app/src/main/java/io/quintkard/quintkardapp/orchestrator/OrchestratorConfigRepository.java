package io.quintkard.quintkardapp.orchestrator;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface OrchestratorConfigRepository extends Repository<OrchestratorConfig, UUID> {

    OrchestratorConfig save(OrchestratorConfig orchestratorConfig);

    Optional<OrchestratorConfig> findByUser_UserId(String userId);
}
