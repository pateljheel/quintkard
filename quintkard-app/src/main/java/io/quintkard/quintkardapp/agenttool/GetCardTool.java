package io.quintkard.quintkardapp.agenttool;

import io.quintkard.quintkardapp.card.CardResponse;
import io.quintkard.quintkardapp.card.CardService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class GetCardTool extends AbstractCardAiTool {

    public GetCardTool(CardService cardService, ObjectMapper objectMapper) {
        super(cardService, objectMapper);
    }

    @Override
    public String name() {
        return "get_card";
    }

    @Override
    public String description() {
        return "Get the full content and metadata for a specific card owned by the current user. cardId must be a UUID.";
    }

    @Override
    public Class<?> inputType() {
        return GetCardArgs.class;
    }

    @Override
    public Object execute(AiToolExecutionRequest request) {
        GetCardArgs arguments = arguments(request, GetCardArgs.class);
        return CardResponse.from(cardService.getCard(
                request.userId(),
                requiredUuid(arguments.cardId(), "cardId")
        ));
    }
}
