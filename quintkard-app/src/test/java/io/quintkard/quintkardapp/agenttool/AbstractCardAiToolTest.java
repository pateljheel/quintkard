package io.quintkard.quintkardapp.agenttool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import io.quintkard.quintkardapp.card.CardPriority;
import io.quintkard.quintkardapp.card.CardService;
import io.quintkard.quintkardapp.card.CardStatus;
import io.quintkard.quintkardapp.card.CardType;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AbstractCardAiToolTest {

    private HarnessTool tool;

    @BeforeEach
    void setUp() {
        tool = new HarnessTool(mock(CardService.class), new ObjectMapper());
    }

    @Test
    void argumentsWrapsConversionFailures() {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        doThrow(new IllegalArgumentException("boom"))
                .when(objectMapper)
                .convertValue(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(Integer.class));
        HarnessTool failingTool = new HarnessTool(mock(CardService.class), objectMapper);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> failingTool.arguments(
                        new AiToolExecutionRequest("admin", "conv", new LinkedHashMap<>(Map.of("value", "x"))),
                        Integer.class
                )
        );

        assertEquals("Invalid arguments for tool 'harness_tool'", exception.getMessage());
    }

    @Test
    void normalizeLimitUsesDefaultAndMaxBounds() {
        assertEquals(5, tool.normalizeLimit(null));
        assertEquals(5, tool.normalizeLimit(0));
        assertEquals(10, tool.normalizeLimit(50));
        assertEquals(7, tool.normalizeLimit(7));
    }

    @Test
    void requiredAndOptionalUuidHandleMissingAndInvalidValues() {
        IllegalArgumentException requiredException = assertThrows(
                IllegalArgumentException.class,
                () -> tool.requiredUuid("   ", "cardId")
        );
        assertEquals("cardId is required", requiredException.getMessage());

        IllegalArgumentException invalidException = assertThrows(
                IllegalArgumentException.class,
                () -> tool.optionalUuid("not-a-uuid", "sourceMessageId")
        );
        assertEquals("sourceMessageId must be a valid UUID", invalidException.getMessage());

        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, tool.requiredUuid(" " + uuid + " ", "cardId"));
        assertNull(tool.optionalUuid("   ", "sourceMessageId"));
    }

    @Test
    void optionalIsoDateHandlesBlankAndInvalidValues() {
        assertNull(tool.optionalIsoDate("   ", "dueDate"));
        assertEquals(LocalDate.of(2026, 5, 29), tool.optionalIsoDate("2026-05-29", "dueDate"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tool.optionalIsoDate("29th May", "dueDate")
        );
        assertEquals("dueDate must be in YYYY-MM-DD format", exception.getMessage());
    }

    @Test
    void parseEnumsHandleOptionalRequiredAndInvalidValues() {
        assertNull(tool.parseCardType("   ", false));
        assertEquals(CardType.ALERT, tool.parseCardType("alert", true));
        assertEquals(CardStatus.DONE, tool.parseCardStatus("done", true));
        assertEquals(CardPriority.HIGH, tool.parseCardPriority("high", true));

        IllegalArgumentException requiredException = assertThrows(
                IllegalArgumentException.class,
                () -> tool.parseCardStatus("   ", true)
        );
        assertEquals("status is required", requiredException.getMessage());

        IllegalArgumentException invalidException = assertThrows(
                IllegalArgumentException.class,
                () -> tool.parseCardPriority("urgent-ish", true)
        );
        org.junit.jupiter.api.Assertions.assertTrue(invalidException.getMessage().startsWith("priority must be one of ["));
    }

    @Test
    void trimToNullTrimsAndNullsBlankValues() {
        assertNull(tool.trimToNull(null));
        assertNull(tool.trimToNull("   "));
        assertEquals("value", tool.trimToNull("  value  "));
    }

    private static final class HarnessTool extends AbstractCardAiTool {
        private HarnessTool(CardService cardService, ObjectMapper objectMapper) {
            super(cardService, objectMapper);
        }

        @Override
        public String name() {
            return "harness_tool";
        }

        @Override
        public String description() {
            return "Harness";
        }

        @Override
        public Class<?> inputType() {
            return Map.class;
        }

        @Override
        public Object execute(AiToolExecutionRequest request) {
            return null;
        }
    }
}
