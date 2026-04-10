package io.quintkard.quintkardapp.card;

import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface CardSearchRepository {

    Slice<CardSummaryProjection> searchHybridSummariesByUserId(
            String userId,
            CardStatus status,
            CardType cardType,
            Instant updatedAfter,
            Instant updatedBefore,
            String query,
            String embeddingModel,
            float[] queryEmbedding,
            Pageable pageable
    );
}
