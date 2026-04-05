package io.quintkard.quintkardapp.card;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quintkard.quintkardapp.embedding.EmbeddingProperties;
import io.quintkard.quintkardapp.embedding.EmbeddingService;
import io.quintkard.quintkardapp.user.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class CardEmbeddingServiceImplTest {

    private CardRepository cardRepository;
    private CardEmbeddingRepository cardEmbeddingRepository;
    private EmbeddingService embeddingService;
    private CardChunkingStrategyRegistry chunkingStrategyRegistry;
    private CardEmbeddingServiceImpl service;

    @BeforeEach
    void setUp() {
        cardRepository = mock(CardRepository.class);
        cardEmbeddingRepository = mock(CardEmbeddingRepository.class);
        embeddingService = mock(EmbeddingService.class);
        chunkingStrategyRegistry = mock(CardChunkingStrategyRegistry.class);
        service = new CardEmbeddingServiceImpl(
                cardRepository,
                cardEmbeddingRepository,
                embeddingService,
                chunkingStrategyRegistry,
                new EmbeddingProperties("gemini-embedding-001", "summary-and-content-v1", 2, 3, 0.45)
        );
    }

    @Test
    void reindexCardDeletesExistingEmbeddingsAndReturnsEmptyWhenNoChunks() {
        UUID cardId = UUID.randomUUID();
        Card card = card(cardId);
        CardChunkingStrategy strategy = mock(CardChunkingStrategy.class);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(chunkingStrategyRegistry.get("summary-and-content-v1")).thenReturn(strategy);
        when(strategy.chunk(card)).thenReturn(List.of());

        List<CardEmbedding> result = service.reindexCard(cardId);

        assertEquals(List.of(), result);
        verify(cardEmbeddingRepository).deleteByCard_Id(cardId);
        verifyNoInteractions(embeddingService);
    }

    @Test
    void reindexCardPartitionsChunksAndBuildsEmbeddings() {
        UUID cardId = UUID.randomUUID();
        Card card = card(cardId);
        CardChunkingStrategy strategy = mock(CardChunkingStrategy.class);
        List<TextChunk> chunks = List.of(
                new TextChunk(0, TextChunkType.SUMMARY, "summary"),
                new TextChunk(1, TextChunkType.CONTENT, "content one"),
                new TextChunk(2, TextChunkType.CONTENT, "content two")
        );
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(chunkingStrategyRegistry.get("summary-and-content-v1")).thenReturn(strategy);
        when(strategy.chunk(card)).thenReturn(chunks);
        when(embeddingService.embedAll(List.of("summary", "content one")))
                .thenReturn(List.of(new float[] {1f, 0f, 0f}, new float[] {0f, 1f, 0f}));
        when(embeddingService.embedAll(List.of("content two")))
                .thenReturn(List.of(new float[] {0f, 0f, 1f}));
        when(cardEmbeddingRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<CardEmbedding> result = service.reindexCard(cardId);

        verify(cardEmbeddingRepository).deleteByCard_Id(cardId);
        verify(embeddingService).embedAll(List.of("summary", "content one"));
        verify(embeddingService).embedAll(List.of("content two"));

        ArgumentCaptor<List<CardEmbedding>> captor = ArgumentCaptor.forClass(List.class);
        verify(cardEmbeddingRepository).saveAll(captor.capture());
        List<CardEmbedding> saved = captor.getValue();
        assertEquals(3, saved.size());
        assertEquals(TextChunkType.SUMMARY, saved.getFirst().getChunkType());
        assertEquals("summary", saved.getFirst().getChunkText());
        assertArrayEquals(new float[] {1f, 0f, 0f}, saved.getFirst().getEmbeddingVector());
        assertEquals(3, result.size());
    }

    @Test
    void reindexCardRejectsUnexpectedVectorDimensions() {
        UUID cardId = UUID.randomUUID();
        Card card = card(cardId);
        CardChunkingStrategy strategy = mock(CardChunkingStrategy.class);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(chunkingStrategyRegistry.get("summary-and-content-v1")).thenReturn(strategy);
        when(strategy.chunk(card)).thenReturn(List.of(new TextChunk(0, TextChunkType.CONTENT, "content")));
        when(embeddingService.embedAll(List.of("content"))).thenReturn(List.of(new float[] {1f, 2f}));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.reindexCard(cardId));

        assertEquals(
                "Embedding model 'gemini-embedding-001' returned 2 dimensions, expected 3",
                exception.getMessage()
        );
    }

    @Test
    void deleteCardEmbeddingsDelegatesToRepository() {
        UUID cardId = UUID.randomUUID();

        service.deleteCardEmbeddings(cardId);

        verify(cardEmbeddingRepository).deleteByCard_Id(cardId);
    }

    private Card card(UUID cardId) {
        Card card = new Card(
                new User("admin", "Admin", "admin@example.com", "hash", false),
                "Title",
                "Summary",
                "Content",
                CardType.TASK,
                CardStatus.OPEN,
                CardPriority.MEDIUM,
                null,
                null
        );
        ReflectionTestUtils.setField(card, "id", cardId);
        return card;
    }
}
