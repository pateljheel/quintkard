package io.quintkard.quintkardapp.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.quintkard.quintkardapp.card.Card;
import io.quintkard.quintkardapp.card.CardController;
import io.quintkard.quintkardapp.card.CardPriority;
import io.quintkard.quintkardapp.card.CardRequest;
import io.quintkard.quintkardapp.card.CardService;
import io.quintkard.quintkardapp.card.CardStatus;
import io.quintkard.quintkardapp.card.CardType;
import io.quintkard.quintkardapp.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = {CardController.class, CsrfController.class})
@Import(SecurityConfig.class)
class CsrfSecurityTest {

    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final String CSRF_PARAMETER_NAME = "_csrf";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private SecurityCorsProperties securityCorsProperties;

    @MockitoBean
    @SuppressWarnings("unused")
    private PasswordEncoder passwordEncoder;

    @Test
    void csrfEndpointReturnsTokenAndCookie() throws Exception {
        when(securityCorsProperties.getAllowedOrigins()).thenReturn(java.util.List.of("http://localhost:3000"));

        mockMvc.perform(get("/api/csrf").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value(CSRF_HEADER_NAME))
                .andExpect(jsonPath("$.parameterName").value(CSRF_PARAMETER_NAME))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(cookie().exists(CSRF_COOKIE_NAME));
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        when(securityCorsProperties.getAllowedOrigins()).thenReturn(java.util.List.of("http://localhost:3000"));

        mockMvc.perform(post("/api/cards")
                        .with(user("admin"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(cardRequestJson()))
                .andExpect(status().isForbidden());

        verify(cardService, never()).createCard(eq("admin"), any(CardRequest.class));
    }

    @Test
    void postWithCsrfTokenFromEndpointIsAccepted() throws Exception {
        when(securityCorsProperties.getAllowedOrigins()).thenReturn(java.util.List.of("http://localhost:3000"));
        when(cardService.createCard(eq("admin"), any(CardRequest.class))).thenReturn(card());

        MvcResult csrfResult = mockMvc.perform(get("/api/csrf").with(user("admin")))
                .andExpect(status().isOk())
                .andReturn();

        String csrfPayload = csrfResult.getResponse().getContentAsString();
        JsonNode payload = objectMapper.readTree(csrfPayload);
        String token = payload.get("token").textValue();
        jakarta.servlet.http.Cookie csrfCookie =
                csrfResult.getResponse().getCookie(CSRF_COOKIE_NAME);

        mockMvc.perform(post("/api/cards")
                        .with(user("admin"))
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(cardRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("admin"))
                .andExpect(jsonPath("$.title").value("Follow up"))
                .andExpect(jsonPath("$.cardType").value("FOLLOW_UP"));

        verify(cardService).createCard(eq("admin"), any(CardRequest.class));
    }

    private static String cardRequestJson() {
        return """
                {
                  "title": "Follow up",
                  "summary": "Invoice",
                  "content": "Review discrepancy",
                  "cardType": "FOLLOW_UP",
                  "status": "OPEN",
                  "priority": "MEDIUM",
                  "dueDate": "2026-05-29"
                }
                """;
    }

    private static Card card() {
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
        ReflectionTestUtils.setField(card, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(card, "createdAt", Instant.parse("2026-04-05T00:00:00Z"));
        ReflectionTestUtils.setField(card, "updatedAt", Instant.parse("2026-04-05T01:00:00Z"));
        return card;
    }
}
