package io.quintkard.quintkardapp.agenttool;

import io.quintkard.quintkardapp.card.CardPriority;
import io.quintkard.quintkardapp.card.CardService;
import io.quintkard.quintkardapp.card.CardStatus;
import io.quintkard.quintkardapp.card.CardType;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;

abstract class AbstractCardAiTool implements AiTool {

    protected static final int DEFAULT_LIMIT = 5;
    protected static final int MAX_LIMIT = 10;

    protected final CardService cardService;
    private final ObjectMapper objectMapper;

    protected AbstractCardAiTool(CardService cardService, ObjectMapper objectMapper) {
        this.cardService = cardService;
        this.objectMapper = objectMapper;
    }

    protected <T> T arguments(AiToolExecutionRequest request, Class<T> type) {
        try {
            return objectMapper.convertValue(request.arguments(), type);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid arguments for tool '%s'".formatted(name()), exception);
        }
    }

    protected int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requestedLimit, MAX_LIMIT);
    }

    protected UUID requiredUuid(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        try {
            return UUID.fromString(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(fieldName + " must be a valid UUID");
        }
    }

    protected UUID optionalUuid(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return UUID.fromString(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(fieldName + " must be a valid UUID");
        }
    }

    protected LocalDate optionalIsoDate(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(fieldName + " must be in YYYY-MM-DD format");
        }
    }

    protected CardType parseCardType(String value, boolean required) {
        return parseEnum(value, CardType.class, "cardType", required);
    }

    protected CardStatus parseCardStatus(String value, boolean required) {
        return parseEnum(value, CardStatus.class, "status", required);
    }

    protected CardPriority parseCardPriority(String value, boolean required) {
        return parseEnum(value, CardPriority.class, "priority", required);
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> enumType, String fieldName, boolean required) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            if (required) {
                throw new IllegalArgumentException(fieldName + " is required");
            }
            return null;
        }

        try {
            return Enum.valueOf(enumType, normalized.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(fieldName + " must be one of " + java.util.Arrays.toString(enumType.getEnumConstants()));
        }
    }
}
