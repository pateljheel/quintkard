package io.quintkard.quintkardapp.card;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface CardSearchRepository {

    Slice<CardSummaryProjection> searchHybridSummariesByUserId(
            String userId,
            CardStatus status,
            String query,
            String embeddingModel,
            float[] queryEmbedding,
            Pageable pageable
    );
}
