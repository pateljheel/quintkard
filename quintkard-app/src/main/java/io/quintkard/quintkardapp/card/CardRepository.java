package io.quintkard.quintkardapp.card;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

public interface CardRepository extends Repository<Card, UUID>, JpaSpecificationExecutor<Card>, CardSearchRepository {

    Card save(Card card);

    Optional<Card> findByIdAndUser_UserId(UUID id, String userId);

    void delete(Card card);
}
