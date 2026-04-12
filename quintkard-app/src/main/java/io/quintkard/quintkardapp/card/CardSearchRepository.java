package io.quintkard.quintkardapp.card;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface CardSearchRepository {

    Slice<CardSummaryProjection> searchHybridSummaries(
            CardFilter filter,
            String embeddingModel,
            float[] queryEmbedding,
            Pageable pageable
    );
}
