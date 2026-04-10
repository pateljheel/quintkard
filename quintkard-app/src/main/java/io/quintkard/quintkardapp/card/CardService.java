package io.quintkard.quintkardapp.card;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Slice;

public interface CardService {

    Card createCard(String userId, CardRequest request);

    Card updateCard(String userId, UUID cardId, CardRequest request);

    void deleteCard(String userId, UUID cardId);

    Card getCard(String userId, UUID cardId);

    Card changeCardStatus(String userId, UUID cardId, CardStatus status);

    Slice<CardSummaryProjection> listCards(
            String userId,
            int page,
            int size,
            String query,
            CardStatus status,
            CardType cardType,
            Instant updatedAfter,
            Instant updatedBefore
    );

//    List<Card> searchCards(String userId, String query, int limit);
}
