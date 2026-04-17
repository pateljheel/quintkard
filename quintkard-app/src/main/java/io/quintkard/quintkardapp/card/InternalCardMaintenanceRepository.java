package io.quintkard.quintkardapp.card;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface InternalCardMaintenanceRepository extends Repository<Card, UUID> {

    Optional<Card> findById(UUID id);
}
