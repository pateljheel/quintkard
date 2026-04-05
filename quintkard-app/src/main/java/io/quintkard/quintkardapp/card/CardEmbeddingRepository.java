package io.quintkard.quintkardapp.card;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardEmbeddingRepository extends JpaRepository<CardEmbedding, UUID> {

    void deleteByCard_Id(UUID cardId);

    List<CardEmbedding> findByCard_IdOrderByChunkIndex(UUID cardId);
}
