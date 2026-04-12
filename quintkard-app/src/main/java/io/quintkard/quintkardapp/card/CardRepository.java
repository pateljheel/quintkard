package io.quintkard.quintkardapp.card;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CardRepository extends JpaRepository<Card, UUID>, JpaSpecificationExecutor<Card>, CardSearchRepository {

    Optional<Card> findByIdAndUser_UserId(UUID id, String userId);
}
