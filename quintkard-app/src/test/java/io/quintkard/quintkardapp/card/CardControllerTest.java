package io.quintkard.quintkardapp.card;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.quintkard.quintkardapp.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CardControllerTest {

    private CardService cardService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cardService = mock(CardService.class);
        CardController controller = new CardController(cardService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
    }

    @Test
    void listCardsUsesAuthenticatedUser() throws Exception {
        UUID cardId = UUID.randomUUID();
        CardSummaryProjection summary = mock(CardSummaryProjection.class);
        when(summary.getId()).thenReturn(cardId);
        when(summary.getUserId()).thenReturn("admin");
        when(summary.getTitle()).thenReturn("Follow up");
        when(summary.getSummary()).thenReturn("Invoice");
        when(summary.getCardType()).thenReturn(CardType.FOLLOW_UP);
        when(summary.getStatus()).thenReturn(CardStatus.OPEN);
        when(summary.getPriority()).thenReturn(CardPriority.MEDIUM);
        when(summary.getDueDate()).thenReturn(LocalDate.of(2026, 5, 29));
        when(summary.getCreatedAt()).thenReturn(Instant.parse("2026-04-05T00:00:00Z"));
        when(summary.getUpdatedAt()).thenReturn(Instant.parse("2026-04-05T01:00:00Z"));
        when(cardService.listCards("admin", 1, 10, "invoice", CardStatus.OPEN))
                .thenReturn(new SliceImpl<>(java.util.List.of(summary), PageRequest.of(1, 10), false));

        mockMvc.perform(authorized(get("/api/cards"))
                        .param("page", "1")
                        .param("size", "10")
                        .param("query", "invoice")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(cardId.toString()))
                .andExpect(jsonPath("$.items[0].title").value("Follow up"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10));

        verify(cardService).listCards("admin", 1, 10, "invoice", CardStatus.OPEN);
    }

    @Test
    void createCardReturnsCreatedResponse() throws Exception {
        Card card = new Card(
                new User("admin", "Admin", "admin@example.com", "hash", false),
                "Follow up",
                "Invoice",
                "Review discrepancy",
                CardType.FOLLOW_UP,
                CardStatus.OPEN,
                CardPriority.MEDIUM,
                LocalDate.of(2026, 5, 29),
                null
        );
        UUID cardId = UUID.randomUUID();
        ReflectionTestUtils.setField(card, "id", cardId);
        ReflectionTestUtils.setField(card, "createdAt", Instant.parse("2026-04-05T00:00:00Z"));
        ReflectionTestUtils.setField(card, "updatedAt", Instant.parse("2026-04-05T01:00:00Z"));
        when(cardService.createCard(org.mockito.ArgumentMatchers.eq("admin"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(card);

        mockMvc.perform(authorized(post("/api/cards"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Follow up",
                                  "summary": "Invoice",
                                  "content": "Review discrepancy",
                                  "cardType": "FOLLOW_UP",
                                  "status": "OPEN",
                                  "priority": "MEDIUM",
                                  "dueDate": "2026-05-29"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(cardId.toString()))
                .andExpect(jsonPath("$.userId").value("admin"))
                .andExpect(jsonPath("$.cardType").value("FOLLOW_UP"));
    }

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder builder) {
        return builder.principal(new TestingAuthenticationToken("admin", "password"));
    }
}
