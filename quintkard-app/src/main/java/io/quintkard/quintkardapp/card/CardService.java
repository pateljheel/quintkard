package io.quintkard.quintkardapp.card;

import java.util.UUID;
import org.springframework.data.domain.Slice;

public interface CardService {

    Card createCard(String userId, CardRequest request);

    Card updateCard(String userId, UUID cardId, CardRequest request);

    void deleteCard(String userId, UUID cardId);

    Card getCard(String userId, UUID cardId);

    Card changeCardStatus(String userId, UUID cardId, CardStatus status);

    Slice<CardSummaryProjection> listCards(CardFilter filter, int page, int size);

//    List<Card> searchCards(String userId, String query, int limit);
}
