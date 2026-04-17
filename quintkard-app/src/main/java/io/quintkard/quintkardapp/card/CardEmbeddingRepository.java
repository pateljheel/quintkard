package io.quintkard.quintkardapp.card;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface CardEmbeddingRepository extends Repository<CardEmbedding, UUID> {

    List<CardEmbedding> saveAll(Iterable<CardEmbedding> embeddings);

    void deleteByCard_Id(UUID cardId);

    List<CardEmbedding> findByCard_IdOrderByChunkIndex(UUID cardId);
}
