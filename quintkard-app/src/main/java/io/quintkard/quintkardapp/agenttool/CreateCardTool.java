package io.quintkard.quintkardapp.agenttool;

import io.quintkard.quintkardapp.card.CardRequest;
import io.quintkard.quintkardapp.card.CardPriority;
import io.quintkard.quintkardapp.card.CardResponse;
import io.quintkard.quintkardapp.card.CardService;
import io.quintkard.quintkardapp.card.CardStatus;
import io.quintkard.quintkardapp.card.CardType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class CreateCardTool extends AbstractCardAiTool {

    public CreateCardTool(CardService cardService, ObjectMapper objectMapper) {
        super(cardService, objectMapper);
    }

    @Override
    public String name() {
        return "create_card";
    }

    @Override
    public String description() {
        return "Create a new card for the current user. Required fields: title, content. Optional fields: summary, cardType, status, priority, dueDate, sourceMessageId. cardType must be one of TASK, NOTE, REMINDER, DECISION, FOLLOW_UP, ALERT. status must be one of OPEN, IN_PROGRESS, DONE, ARCHIVED. priority must be one of LOW, MEDIUM, HIGH, URGENT. dueDate must be YYYY-MM-DD.";
    }

    @Override
    public Class<?> inputType() {
        return CreateCardArgs.class;
    }

    @Override
    public Object execute(AiToolExecutionRequest request) {
        CreateCardArgs arguments = arguments(request, CreateCardArgs.class);
        CardType cardType = parseCardType(arguments.cardType(), false);
        CardStatus status = parseCardStatus(arguments.status(), false);
        CardPriority priority = parseCardPriority(arguments.priority(), false);
        CardRequest cardRequest = new CardRequest(
                trimToNull(arguments.title()),
                arguments.summary(),
                trimToNull(arguments.content()),
                cardType == null ? CardType.TASK : cardType,
                status == null ? CardStatus.OPEN : status,
                priority == null ? CardPriority.MEDIUM : priority,
                optionalIsoDate(arguments.dueDate(), "dueDate"),
                optionalUuid(arguments.sourceMessageId(), "sourceMessageId")
        );
        return CardResponse.from(cardService.createCard(request.userId(), cardRequest));
    }
}
