package io.quintkard.quintkardapp.agenttool;

import io.quintkard.quintkardapp.card.CardResponse;
import io.quintkard.quintkardapp.card.CardService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ChangeCardStatusTool extends AbstractCardAiTool {

    public ChangeCardStatusTool(CardService cardService, ObjectMapper objectMapper) {
        super(cardService, objectMapper);
    }

    @Override
    public String name() {
        return "change_card_status";
    }

    @Override
    public String description() {
        return "Change only the status of an existing card owned by the current user. cardId must be a UUID. status must be one of OPEN, IN_PROGRESS, DONE, ARCHIVED.";
    }

    @Override
    public Class<?> inputType() {
        return ChangeCardStatusArgs.class;
    }

    @Override
    public Object execute(AiToolExecutionRequest request) {
        ChangeCardStatusArgs arguments = arguments(request, ChangeCardStatusArgs.class);
        return CardResponse.from(cardService.changeCardStatus(
                request.userId(),
                requiredUuid(arguments.cardId(), "cardId"),
                parseCardStatus(arguments.status(), true)
        ));
    }
}
