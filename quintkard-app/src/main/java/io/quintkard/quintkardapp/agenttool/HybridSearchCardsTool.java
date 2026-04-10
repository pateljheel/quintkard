package io.quintkard.quintkardapp.agenttool;

import io.quintkard.quintkardapp.card.CardService;
import io.quintkard.quintkardapp.card.CardSummaryProjection;
import io.quintkard.quintkardapp.card.CardSliceResponse;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class HybridSearchCardsTool extends AbstractCardAiTool {

    public HybridSearchCardsTool(CardService cardService, ObjectMapper objectMapper) {
        super(cardService, objectMapper);
    }

    @Override
    public String name() {
        return "hybrid_search_cards";
    }

    @Override
    public String description() {
        return "Search the current user's cards using hybrid keyword and semantic ranking. query is required. status is optional and must be one of OPEN, IN_PROGRESS, DONE, ARCHIVED.";
    }

    @Override
    public Class<?> inputType() {
        return HybridSearchCardsArgs.class;
    }

    @Override
    public Object execute(AiToolExecutionRequest request) {
        HybridSearchCardsArgs arguments = arguments(request, HybridSearchCardsArgs.class);
        if (arguments.query() == null || arguments.query().isBlank()) {
            throw new IllegalArgumentException("query is required");
        }

        Slice<CardSummaryProjection> cards = cardService.listCards(
                request.userId(),
                0,
                normalizeLimit(arguments.limit()),
                arguments.query(),
                parseCardStatus(arguments.status(), false),
                null,
                null,
                null
        );
        return CardSliceResponse.from(cards);
    }
}
