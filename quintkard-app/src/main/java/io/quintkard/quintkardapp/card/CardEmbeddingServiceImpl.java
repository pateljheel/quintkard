package io.quintkard.quintkardapp.card;

import io.quintkard.quintkardapp.embedding.EmbeddingProperties;
import io.quintkard.quintkardapp.embedding.EmbeddingService;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardEmbeddingServiceImpl implements CardEmbeddingService {

    private final InternalCardMaintenanceRepository cardMaintenanceRepository;
    private final CardEmbeddingRepository cardEmbeddingRepository;
    private final EmbeddingService embeddingService;
    private final CardChunkingStrategyRegistry chunkingStrategyRegistry;
    private final EmbeddingProperties embeddingProperties;

    public CardEmbeddingServiceImpl(
            InternalCardMaintenanceRepository cardMaintenanceRepository,
            CardEmbeddingRepository cardEmbeddingRepository,
            EmbeddingService embeddingService,
            CardChunkingStrategyRegistry chunkingStrategyRegistry,
            EmbeddingProperties embeddingProperties
    ) {
        this.cardMaintenanceRepository = cardMaintenanceRepository;
        this.cardEmbeddingRepository = cardEmbeddingRepository;
        this.embeddingService = embeddingService;
        this.chunkingStrategyRegistry = chunkingStrategyRegistry;
        this.embeddingProperties = embeddingProperties;
    }

    @Override
    @Transactional
    public List<CardEmbedding> reindexCard(UUID cardId) {
        Card card = cardMaintenanceRepository.findById(cardId)
                .orElseThrow(() -> new NoSuchElementException("Card not found: " + cardId));

        CardChunkingStrategy chunkingStrategy = chunkingStrategyRegistry.get(embeddingProperties.chunkingStrategy());
        List<TextChunk> chunks = chunkingStrategy.chunk(card);

        cardEmbeddingRepository.deleteByCard_Id(cardId);
        if (chunks.isEmpty()) {
            return List.of();
        }

        List<CardEmbedding> allEmbeddings = new ArrayList<>();
        for (List<TextChunk> batch : partition(chunks, embeddingProperties.batchSize())) {
            List<String> texts = batch.stream()
                    .map(TextChunk::text)
                    .toList();
            List<float[]> vectors = embeddingService.embedAll(texts);
            validateVectorDimensions(vectors);

            for (int i = 0; i < batch.size(); i++) {
                TextChunk chunk = batch.get(i);
                allEmbeddings.add(new CardEmbedding(
                        card,
                        embeddingProperties.model(),
                        embeddingProperties.chunkingStrategy(),
                        chunk.chunkIndex(),
                        chunk.chunkType(),
                        chunk.text(),
                        vectors.get(i)
                ));
            }
        }

        return cardEmbeddingRepository.saveAll(allEmbeddings);
    }

    @Override
    @Transactional
    public void deleteCardEmbeddings(UUID cardId) {
        cardEmbeddingRepository.deleteByCard_Id(cardId);
    }

    private List<List<TextChunk>> partition(List<TextChunk> chunks, int batchSize) {
        List<List<TextChunk>> partitions = new ArrayList<>();
        for (int start = 0; start < chunks.size(); start += batchSize) {
            partitions.add(chunks.subList(start, Math.min(chunks.size(), start + batchSize)));
        }
        return partitions;
    }

    private void validateVectorDimensions(List<float[]> vectors) {
        int expectedDimensions = embeddingProperties.dimensions();
        for (float[] vector : vectors) {
            if (vector.length != expectedDimensions) {
                throw new IllegalStateException(
                        "Embedding model '%s' returned %d dimensions, expected %d".formatted(
                                embeddingProperties.model(),
                                vector.length,
                                expectedDimensions
                        )
                );
            }
        }
    }
}
