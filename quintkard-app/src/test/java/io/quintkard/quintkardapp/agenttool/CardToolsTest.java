package io.quintkard.quintkardapp.agenttool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quintkard.quintkardapp.card.Card;
import io.quintkard.quintkardapp.card.CardPriority;
import io.quintkard.quintkardapp.card.CardRequest;
import io.quintkard.quintkardapp.card.CardResponse;
import io.quintkard.quintkardapp.card.CardService;
import io.quintkard.quintkardapp.card.CardStatus;
import io.quintkard.quintkardapp.card.CardSummaryProjection;
import io.quintkard.quintkardapp.card.CardType;
import io.quintkard.quintkardapp.card.CardSliceResponse;
import io.quintkard.quintkardapp.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

class CardToolsTest {

    private CardService cardService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        cardService = mock(CardService.class);
        objectMapper = new ObjectMapper();
    }

    @Test
    void createCardToolAppliesDefaultsAndTrimsInput() {
        CreateCardTool tool = new CreateCardTool(cardService, objectMapper);
        Card card = card(UUID.randomUUID(), "Follow up on invoice", CardType.TASK, CardStatus.OPEN, CardPriority.MEDIUM);
        when(cardService.createCard(eq("admin"), any(CardRequest.class))).thenReturn(card);

        CardResponse response = (CardResponse) tool.execute(new AiToolExecutionRequest(
                "admin",
                "conv-1",
                Map.of(
                        "title", "  Follow up on invoice  ",
                        "content", "  Check last August billing discrepancy  "
                )
        ));

        ArgumentCaptor<CardRequest> requestCaptor = ArgumentCaptor.forClass(CardRequest.class);
        verify(cardService).createCard(eq("admin"), requestCaptor.capture());
        CardRequest cardRequest = requestCaptor.getValue();
        assertEquals("Follow up on invoice", cardRequest.title());
        assertEquals("Check last August billing discrepancy", cardRequest.content());
        assertEquals(CardType.TASK, cardRequest.cardType());
        assertEquals(CardStatus.OPEN, cardRequest.status());
        assertEquals(CardPriority.MEDIUM, cardRequest.priority());
        assertEquals(card.getId(), response.id());
    }

    @Test
    void createCardToolRejectsNaturalLanguageDate() {
        CreateCardTool tool = new CreateCardTool(cardService, objectMapper);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tool.execute(new AiToolExecutionRequest(
                        "admin",
                        "conv-1",
                        Map.of(
                                "title", "Reminder",
                                "content", "Need action",
                                "dueDate", "29th May"
                        )
                ))
        );

        assertEquals("dueDate must be in YYYY-MM-DD format", exception.getMessage());
    }

    @Test
    void updateCardToolParsesUuidAndEnums() {
        UpdateCardTool tool = new UpdateCardTool(cardService, objectMapper);
        UUID cardId = UUID.randomUUID();
        UUID sourceMessageId = UUID.randomUUID();
        Card card = card(cardId, "Updated", CardType.ALERT, CardStatus.IN_PROGRESS, CardPriority.HIGH);
        when(cardService.updateCard(eq("admin"), eq(cardId), any(CardRequest.class))).thenReturn(card);

        CardResponse response = (CardResponse) tool.execute(new AiToolExecutionRequest(
                "admin",
                "conv-1",
                Map.of(
                        "cardId", cardId.toString(),
                        "title", "  Updated  ",
                        "summary", "Escalated",
                        "content", "  Investigate immediately  ",
                        "cardType", "alert",
                        "status", "in_progress",
                        "priority", "high",
                        "dueDate", "2026-05-29",
                        "sourceMessageId", sourceMessageId.toString()
                )
        ));

        ArgumentCaptor<CardRequest> requestCaptor = ArgumentCaptor.forClass(CardRequest.class);
        verify(cardService).updateCard(eq("admin"), eq(cardId), requestCaptor.capture());
        CardRequest cardRequest = requestCaptor.getValue();
        assertEquals(CardType.ALERT, cardRequest.cardType());
        assertEquals(CardStatus.IN_PROGRESS, cardRequest.status());
        assertEquals(CardPriority.HIGH, cardRequest.priority());
        assertEquals(LocalDate.of(2026, 5, 29), cardRequest.dueDate());
        assertEquals(sourceMessageId, cardRequest.sourceMessageId());
        assertEquals(cardId, response.id());
    }

    @Test
    void changeCardStatusToolRejectsInvalidStatus() {
        ChangeCardStatusTool tool = new ChangeCardStatusTool(cardService, objectMapper);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tool.execute(new AiToolExecutionRequest(
                        "admin",
                        "conv-1",
                        Map.of(
                                "cardId", UUID.randomUUID().toString(),
                                "status", "waiting"
                        )
                ))
        );

        assertTrue(exception.getMessage().startsWith("status must be one of ["));
    }

    @Test
    void getCardToolParsesUuidAndReturnsFullCardResponse() {
        GetCardTool tool = new GetCardTool(cardService, objectMapper);
        UUID cardId = UUID.randomUUID();
        Card card = card(cardId, "Follow up", CardType.FOLLOW_UP, CardStatus.OPEN, CardPriority.MEDIUM);
        when(cardService.getCard("admin", cardId)).thenReturn(card);

        CardResponse response = (CardResponse) tool.execute(new AiToolExecutionRequest(
                "admin",
                "conv-1",
                Map.of("cardId", cardId.toString())
        ));

        verify(cardService).getCard("admin", cardId);
        assertEquals(cardId, response.id());
        assertEquals("Follow up", response.title());
    }

    @Test
    void getCardToolRejectsMissingCardId() {
        GetCardTool tool = new GetCardTool(cardService, objectMapper);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tool.execute(new AiToolExecutionRequest("admin", "conv-1", Map.of("cardId", "   ")))
        );

        assertEquals("cardId is required", exception.getMessage());
    }

    @Test
    void getCardToolRejectsInvalidCardId() {
        GetCardTool tool = new GetCardTool(cardService, objectMapper);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tool.execute(new AiToolExecutionRequest("admin", "conv-1", Map.of("cardId", "not-a-uuid")))
        );

        assertEquals("cardId must be a valid UUID", exception.getMessage());
    }

    @Test
    void getCardToolExposesMetadata() {
        GetCardTool tool = new GetCardTool(cardService, objectMapper);

        assertEquals("get_card", tool.name());
        assertTrue(tool.description().contains("cardId must be a UUID"));
        assertSame(GetCardArgs.class, tool.inputType());
    }

    @Test
    void hybridSearchCardsToolNormalizesLimitAndReturnsSliceResponse() {
        HybridSearchCardsTool tool = new HybridSearchCardsTool(cardService, objectMapper);
        CardSummaryProjection summary = mock(CardSummaryProjection.class);
        when(summary.getId()).thenReturn(UUID.randomUUID());
        when(summary.getUserId()).thenReturn("admin");
        when(summary.getTitle()).thenReturn("Follow up");
        when(summary.getSummary()).thenReturn("Invoice");
        when(summary.getCardType()).thenReturn(CardType.FOLLOW_UP);
        when(summary.getStatus()).thenReturn(CardStatus.OPEN);
        when(summary.getPriority()).thenReturn(CardPriority.MEDIUM);
        when(cardService.listCards("admin", 0, 10, "invoice", CardStatus.OPEN))
                .thenReturn(new SliceImpl<>(List.of(summary)));

        CardSliceResponse response = (CardSliceResponse) tool.execute(new AiToolExecutionRequest(
                "admin",
                "conv-1",
                Map.of(
                        "query", "invoice",
                        "status", "open",
                        "limit", 99
                )
        ));

        assertEquals(1, response.items().size());
        assertEquals("Follow up", response.items().getFirst().title());
        verify(cardService).listCards("admin", 0, 10, "invoice", CardStatus.OPEN);
    }

    @Test
    void hybridSearchCardsToolRejectsBlankQuery() {
        HybridSearchCardsTool tool = new HybridSearchCardsTool(cardService, objectMapper);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tool.execute(new AiToolExecutionRequest(
                        "admin",
                        "conv-1",
                        Map.of("query", "   ")
                ))
        );

        assertEquals("query is required", exception.getMessage());
    }

    private Card card(UUID id, String title, CardType type, CardStatus status, CardPriority priority) {
        User user = new User("admin", "Admin", "admin@example.com", "hash", false);
        Card card = new Card(
                user,
                title,
                "Summary",
                "Content",
                type,
                status,
                priority,
                null,
                null
        );
        ReflectionTestUtils.setField(card, "id", id);
        ReflectionTestUtils.setField(card, "createdAt", Instant.parse("2026-04-05T00:00:00Z"));
        ReflectionTestUtils.setField(card, "updatedAt", Instant.parse("2026-04-05T01:00:00Z"));
        return card;
    }
}
