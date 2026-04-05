package io.quintkard.quintkardapp.card;

import io.quintkard.quintkardapp.embedding.EmbeddingProperties;
import io.quintkard.quintkardapp.embedding.EmbeddingService;
import io.quintkard.quintkardapp.user.User;
import io.quintkard.quintkardapp.user.UserRepository;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardServiceImpl implements CardService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final CardRepository cardRepository;
    private final CardEmbeddingService cardEmbeddingService;
    private final EmbeddingService embeddingService;
    private final EmbeddingProperties embeddingProperties;
    private final UserRepository userRepository;

    public CardServiceImpl(
            CardRepository cardRepository,
            CardEmbeddingService cardEmbeddingService,
            EmbeddingService embeddingService,
            EmbeddingProperties embeddingProperties,
            UserRepository userRepository
    ) {
        this.cardRepository = cardRepository;
        this.cardEmbeddingService = cardEmbeddingService;
        this.embeddingService = embeddingService;
        this.embeddingProperties = embeddingProperties;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Card createCard(String userId, CardRequest request) {
        validateRequest(request);
        Card card = new Card(
                getUser(userId),
                request.title().trim(),
                trimToNull(request.summary()),
                request.content().trim(),
                request.cardType(),
                request.status(),
                request.priority(),
                request.dueDate(),
                request.sourceMessageId()
        );
        Card savedCard = cardRepository.save(card);
        cardEmbeddingService.reindexCard(savedCard.getId());
        return savedCard;
    }

    @Override
    @Transactional
    public Card updateCard(String userId, UUID cardId, CardRequest request) {
        validateRequest(request);
        Card card = getCard(userId, cardId);
        card.update(
                request.title().trim(),
                trimToNull(request.summary()),
                request.content().trim(),
                request.cardType(),
                request.status(),
                request.priority(),
                request.dueDate(),
                request.sourceMessageId()
        );
        cardEmbeddingService.reindexCard(card.getId());
        return card;
    }

    @Override
    @Transactional
    public void deleteCard(String userId, UUID cardId) {
        Card card = getCard(userId, cardId);
        cardEmbeddingService.deleteCardEmbeddings(card.getId());
        cardRepository.delete(card);
    }

    @Override
    @Transactional(readOnly = true)
    public Card getCard(String userId, UUID cardId) {
        return cardRepository.findByIdAndUser_UserId(cardId, userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Card not found for user %s: %s".formatted(userId, cardId)
                ));
    }

    @Override
    @Transactional
    public Card changeCardStatus(String userId, UUID cardId, CardStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Card status is required");
        }

        Card card = getCard(userId, cardId);
        card.changeStatus(status);
        return card;
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<CardSummaryProjection> listCards(String userId, int page, int size, String query, CardStatus status) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizePageSize(size));

        if (query == null || query.isBlank()) {
            if (status == null) {
                return cardRepository.findSummariesByUserUserIdOrderByUpdatedAtDesc(userId, pageable);
            }
            return cardRepository.findSummariesByUserUserIdAndStatusOrderByUpdatedAtDesc(userId, status, pageable);
        }

        return cardRepository.searchHybridSummariesByUserId(
                userId,
                status,
                query.trim(),
                embeddingProperties.model(),
                embeddingService.embed(query.trim()),
                pageable
        );
    }

    private User getUser(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    }

    private void validateRequest(CardRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Card title is required");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Card content is required");
        }
        if (request.cardType() == null) {
            throw new IllegalArgumentException("Card type is required");
        }
        if (request.status() == null) {
            throw new IllegalArgumentException("Card status is required");
        }
        if (request.priority() == null) {
            throw new IllegalArgumentException("Card priority is required");
        }
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
