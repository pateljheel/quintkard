package io.quintkard.quintkardapp.card;

import java.util.List;
import java.util.UUID;

public interface CardEmbeddingService {

    List<CardEmbedding> reindexCard(UUID cardId);

    void deleteCardEmbeddings(UUID cardId);
}
