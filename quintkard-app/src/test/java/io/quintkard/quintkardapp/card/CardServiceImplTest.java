package io.quintkard.quintkardapp.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quintkard.quintkardapp.embedding.EmbeddingProperties;
import io.quintkard.quintkardapp.embedding.EmbeddingService;
import io.quintkard.quintkardapp.user.User;
import io.quintkard.quintkardapp.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

class CardServiceImplTest {

    private CardRepository cardRepository;
    private CardEmbeddingService cardEmbeddingService;
    private EmbeddingService embeddingService;
    private UserRepository userRepository;
    private CardServiceImpl cardService;

    @BeforeEach
    void setUp() {
        cardRepository = mock(CardRepository.class);
        cardEmbeddingService = mock(CardEmbeddingService.class);
        embeddingService = mock(EmbeddingService.class);
        userRepository = mock(UserRepository.class);
        cardService = new CardServiceImpl(
                cardRepository,
                cardEmbeddingService,
                embeddingService,
                new EmbeddingProperties("gemini-embedding-001", "summary-and-content-v1", 16, 3072, 0.45),
                userRepository
        );
    }

    @Test
    void createCardTrimsFieldsAndReindexesSavedCard() {
        User user = new User("admin", "Admin", "admin@example.com", "hash", false);
        UUID cardId = UUID.randomUUID();
        when(userRepository.findByUserId("admin")).thenReturn(Optional.of(user));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
            Card card = invocation.getArgument(0);
            ReflectionTestUtils.setField(card, "id", cardId);
            return card;
        });

        Card saved = cardService.createCard("admin", new CardRequest(
                "  Follow up  ",
                "   ",
                "  Review invoice discrepancy  ",
                CardType.FOLLOW_UP,
                CardStatus.OPEN,
                CardPriority.MEDIUM,
                LocalDate.of(2026, 5, 29),
                null
        ));

        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).save(cardCaptor.capture());
        Card persisted = cardCaptor.getValue();
        assertEquals("Follow up", persisted.getTitle());
        assertNull(persisted.getSummary());
        assertEquals("Review invoice discrepancy", persisted.getContent());
        assertSame(user, persisted.getUser());
        assertEquals(cardId, saved.getId());
        verify(cardEmbeddingService).reindexCard(cardId);
        verifyNoInteractions(embeddingService);
    }

    @Test
    void listCardsWithoutQueryUsesRepositoryListPath() {
        Card entity = card("admin", UUID.randomUUID());
        PageImpl<Card> expectedSlice = new PageImpl<>(List.of(entity));
        when(cardRepository.findAll(
                any(Specification.class),
                any(PageRequest.class)
        ))
                .thenReturn(expectedSlice);

        Slice<CardSummaryProjection> result = cardService.listCards(
                new CardFilter(
                        "admin",
                        "   ",
                        null,
                        CardType.FOLLOW_UP,
                        Instant.parse("2026-04-05T00:00:00Z"),
                        Instant.parse("2026-04-06T00:00:00Z")
                ),
                0,
                25
        );

        assertEquals(1, result.getContent().size());
        assertEquals(entity.getId(), result.getContent().getFirst().getId());
        verify(cardRepository).findAll(
                any(Specification.class),
                eq(PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "updatedAt")))
        );
        verifyNoInteractions(embeddingService);
    }

    @Test
    void listCardsWithQueryEmbedsTextAndUsesHybridSearch() {
        float[] embedding = new float[] {0.1f, 0.2f};
        @SuppressWarnings("unchecked")
        Slice<CardSummaryProjection> expectedSlice = new SliceImpl<>(List.of(mock(CardSummaryProjection.class)));
        when(embeddingService.embed("invoice follow up")).thenReturn(embedding);
        when(cardRepository.searchHybridSummaries(
                eq(new CardFilter(
                        "admin",
                        "invoice follow up",
                        CardStatus.OPEN,
                        CardType.FOLLOW_UP,
                        Instant.parse("2026-04-05T00:00:00Z"),
                        Instant.parse("2026-04-06T00:00:00Z")
                )),
                eq("gemini-embedding-001"),
                eq(embedding),
                any(PageRequest.class)
        )).thenReturn(expectedSlice);

        Slice<CardSummaryProjection> result =
                cardService.listCards(
                        new CardFilter(
                                "admin",
                                "  invoice follow up  ",
                                CardStatus.OPEN,
                                CardType.FOLLOW_UP,
                                Instant.parse("2026-04-05T00:00:00Z"),
                                Instant.parse("2026-04-06T00:00:00Z")
                        ),
                        -2,
                        500
                );

        assertSame(expectedSlice, result);
        verify(embeddingService).embed("invoice follow up");
        verify(cardRepository).searchHybridSummaries(
                eq(new CardFilter(
                        "admin",
                        "invoice follow up",
                        CardStatus.OPEN,
                        CardType.FOLLOW_UP,
                        Instant.parse("2026-04-05T00:00:00Z"),
                        Instant.parse("2026-04-06T00:00:00Z")
                )),
                eq("gemini-embedding-001"),
                eq(embedding),
                eq(PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "updatedAt")))
        );
    }

    @Test
    void changeCardStatusRejectsNullStatus() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cardService.changeCardStatus("admin", UUID.randomUUID(), null)
        );

        assertEquals("Card status is required", exception.getMessage());
        verify(cardRepository, never()).findByIdAndUser_UserId(any(), any());
    }

    @Test
    void createCardRejectsMissingTitle() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cardService.createCard("admin", new CardRequest(
                        "   ",
                        null,
                        "Content",
                        CardType.TASK,
                        CardStatus.OPEN,
                        CardPriority.MEDIUM,
                        null,
                        null
                ))
        );

        assertEquals("Card title is required", exception.getMessage());
        verifyNoInteractions(userRepository, cardRepository, cardEmbeddingService, embeddingService);
    }

    @Test
    void createCardRejectsMissingContent() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cardService.createCard("admin", new CardRequest(
                        "Title",
                        null,
                        "   ",
                        CardType.TASK,
                        CardStatus.OPEN,
                        CardPriority.MEDIUM,
                        null,
                        null
                ))
        );

        assertEquals("Card content is required", exception.getMessage());
    }

    @Test
    void createCardRejectsMissingType() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cardService.createCard("admin", new CardRequest(
                        "Title",
                        null,
                        "Content",
                        null,
                        CardStatus.OPEN,
                        CardPriority.MEDIUM,
                        null,
                        null
                ))
        );

        assertEquals("Card type is required", exception.getMessage());
    }

    @Test
    void createCardRejectsMissingStatus() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cardService.createCard("admin", new CardRequest(
                        "Title",
                        null,
                        "Content",
                        CardType.TASK,
                        null,
                        CardPriority.MEDIUM,
                        null,
                        null
                ))
        );

        assertEquals("Card status is required", exception.getMessage());
    }

    @Test
    void createCardRejectsMissingPriority() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cardService.createCard("admin", new CardRequest(
                        "Title",
                        null,
                        "Content",
                        CardType.TASK,
                        CardStatus.OPEN,
                        null,
                        null,
                        null
                ))
        );

        assertEquals("Card priority is required", exception.getMessage());
    }

    @Test
    void createCardFailsWhenUserMissing() {
        when(userRepository.findByUserId("admin")).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> cardService.createCard("admin", new CardRequest(
                        "Title",
                        null,
                        "Content",
                        CardType.TASK,
                        CardStatus.OPEN,
                        CardPriority.MEDIUM,
                        null,
                        null
                ))
        );

        assertEquals("User not found: admin", exception.getMessage());
    }

    @Test
    void updateCardUpdatesEntityAndReindexes() {
        UUID cardId = UUID.randomUUID();
        Card card = card("admin", cardId);
        when(cardRepository.findByIdAndUser_UserId(cardId, "admin")).thenReturn(Optional.of(card));

        Card updated = cardService.updateCard("admin", cardId, new CardRequest(
                "  Updated title  ",
                "  Updated summary  ",
                "  Updated content  ",
                CardType.ALERT,
                CardStatus.IN_PROGRESS,
                CardPriority.HIGH,
                LocalDate.of(2026, 6, 1),
                UUID.fromString("11111111-1111-1111-1111-111111111111")
        ));

        assertSame(card, updated);
        assertEquals("Updated title", updated.getTitle());
        assertEquals("Updated summary", updated.getSummary());
        assertEquals("Updated content", updated.getContent());
        assertEquals(CardType.ALERT, updated.getCardType());
        assertEquals(CardStatus.IN_PROGRESS, updated.getStatus());
        assertEquals(CardPriority.HIGH, updated.getPriority());
        verify(cardEmbeddingService).reindexCard(cardId);
    }

    @Test
    void deleteCardDeletesEmbeddingsThenCard() {
        UUID cardId = UUID.randomUUID();
        Card card = card("admin", cardId);
        when(cardRepository.findByIdAndUser_UserId(cardId, "admin")).thenReturn(Optional.of(card));

        cardService.deleteCard("admin", cardId);

        verify(cardEmbeddingService).deleteCardEmbeddings(cardId);
        verify(cardRepository).delete(card);
    }

    @Test
    void getCardThrowsWhenNotFoundForUser() {
        UUID cardId = UUID.randomUUID();
        when(cardRepository.findByIdAndUser_UserId(cardId, "admin")).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> cardService.getCard("admin", cardId)
        );

        assertEquals("Card not found for user admin: " + cardId, exception.getMessage());
    }

    @Test
    void changeCardStatusUpdatesExistingCard() {
        UUID cardId = UUID.randomUUID();
        Card card = card("admin", cardId);
        when(cardRepository.findByIdAndUser_UserId(cardId, "admin")).thenReturn(Optional.of(card));

        Card updated = cardService.changeCardStatus("admin", cardId, CardStatus.ARCHIVED);

        assertSame(card, updated);
        assertEquals(CardStatus.ARCHIVED, updated.getStatus());
    }

    @Test
    void listCardsWithoutQueryAndStatusUsesStatusRepositoryPath() {
        Card entity = card("admin", UUID.randomUUID());
        PageImpl<Card> expectedSlice = new PageImpl<>(List.of(entity));
        when(cardRepository.findAll(
                any(Specification.class),
                any(PageRequest.class)
        ))
                .thenReturn(expectedSlice);

        Slice<CardSummaryProjection> result = cardService.listCards(
                new CardFilter("admin", null, CardStatus.DONE, null, null, null),
                0,
                -1
        );

        assertEquals(1, result.getContent().size());
        assertEquals(entity.getId(), result.getContent().getFirst().getId());
        verify(cardRepository).findAll(
                any(Specification.class),
                eq(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt")))
        );
        verifyNoInteractions(embeddingService);
    }

    private Card card(String userId, UUID cardId) {
        Card card = new Card(
                new User(userId, "Admin", "admin@example.com", "hash", false),
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
        assertNotNull(card.getUser());
        return card;
    }
}
