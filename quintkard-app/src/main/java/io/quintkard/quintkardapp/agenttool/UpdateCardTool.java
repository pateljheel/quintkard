package io.quintkard.quintkardapp.agenttool;

import io.quintkard.quintkardapp.card.CardRequest;
import io.quintkard.quintkardapp.card.CardResponse;
import io.quintkard.quintkardapp.card.CardService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class UpdateCardTool extends AbstractCardAiTool {

    public UpdateCardTool(CardService cardService, ObjectMapper objectMapper) {
        super(cardService, objectMapper);
    }

    @Override
    public String name() {
        return "update_card";
    }

    @Override
    public String description() {
        return "Update an existing card owned by the current user. cardId must be a UUID. cardType must be one of TASK, NOTE, REMINDER, DECISION, FOLLOW_UP, ALERT. status must be one of OPEN, IN_PROGRESS, DONE, ARCHIVED. priority must be one of LOW, MEDIUM, HIGH, URGENT. dueDate must be YYYY-MM-DD.";
    }

    @Override
    public Class<?> inputType() {
        return UpdateCardArgs.class;
    }

    @Override
    public Object execute(AiToolExecutionRequest request) {
        UpdateCardArgs arguments = arguments(request, UpdateCardArgs.class);
        CardRequest cardRequest = new CardRequest(
                trimToNull(arguments.title()),
                arguments.summary(),
                trimToNull(arguments.content()),
                parseCardType(arguments.cardType(), true),
                parseCardStatus(arguments.status(), true),
                parseCardPriority(arguments.priority(), true),
                optionalIsoDate(arguments.dueDate(), "dueDate"),
                optionalUuid(arguments.sourceMessageId(), "sourceMessageId")
        );
        return CardResponse.from(cardService.updateCard(
                request.userId(),
                requiredUuid(arguments.cardId(), "cardId"),
                cardRequest
        ));
    }
}
