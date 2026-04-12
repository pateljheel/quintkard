package io.quintkard.quintkardapp.card;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse createCard(
            Authentication authentication,
            @Valid @RequestBody CardRequest request
    ) {
        return CardResponse.from(cardService.createCard(authentication.getName(), request));
    }

    @PutMapping("/{cardId}")
    public CardResponse updateCard(
            Authentication authentication,
            @PathVariable UUID cardId,
            @Valid @RequestBody CardRequest request
    ) {
        return CardResponse.from(cardService.updateCard(authentication.getName(), cardId, request));
    }

    @DeleteMapping("/{cardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(
            Authentication authentication,
            @PathVariable UUID cardId
    ) {
        cardService.deleteCard(authentication.getName(), cardId);
    }

    @GetMapping("/{cardId}")
    public CardResponse getCard(
            Authentication authentication,
            @PathVariable UUID cardId
    ) {
        return CardResponse.from(cardService.getCard(authentication.getName(), cardId));
    }

    @GetMapping
    public CardSliceResponse listCards(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) CardType cardType,
            @RequestParam(required = false) Instant updatedAfter,
            @RequestParam(required = false) Instant updatedBefore
    ) {
        CardFilter filter = new CardFilter(
                authentication.getName(),
                query,
                status,
                cardType,
                updatedAfter,
                updatedBefore
        );
        Slice<CardSummaryProjection> cards = cardService.listCards(filter, page, size);
        return CardSliceResponse.from(cards);
    }
}
