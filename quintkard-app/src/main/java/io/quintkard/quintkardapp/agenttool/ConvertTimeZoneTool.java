package io.quintkard.quintkardapp.agenttool;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ConvertTimeZoneTool implements AiTool {

    private final ObjectMapper objectMapper;

    public ConvertTimeZoneTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "convert_time_zone";
    }

    @Override
    public String description() {
        return "Convert a dateTime from one time zone to another. dateTime must be ISO-8601 like 2026-04-05T15:30:00. fromTimeZone and toTimeZone must be valid IANA zones.";
    }

    @Override
    public Class<?> inputType() {
        return ConvertTimeZoneArgs.class;
    }

    @Override
    public Object execute(AiToolExecutionRequest request) {
        ConvertTimeZoneArgs arguments = objectMapper.convertValue(request.arguments(), ConvertTimeZoneArgs.class);
        String dateTime = trimToNull(arguments.dateTime());
        if (dateTime == null) {
            throw new IllegalArgumentException("dateTime is required");
        }

        ZoneId fromZone = resolveZoneId(arguments.fromTimeZone(), "fromTimeZone");
        ZoneId toZone = resolveZoneId(arguments.toTimeZone(), "toTimeZone");
        ZonedDateTime sourceDateTime = parseDateTime(dateTime, fromZone);
        ZonedDateTime converted = sourceDateTime.withZoneSameInstant(toZone);

        return Map.of(
                "sourceTimeZone", fromZone.getId(),
                "targetTimeZone", toZone.getId(),
                "sourceDateTime", sourceDateTime.toString(),
                "convertedDateTime", converted.toString(),
                "convertedOffsetDateTime", converted.toOffsetDateTime().toString()
        );
    }

    private ZonedDateTime parseDateTime(String value, ZoneId fromZone) {
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(fromZone);
        } catch (DateTimeParseException ignored) {
            // Fall through to local date-time parsing.
        }

        try {
            return LocalDateTime.parse(value).atZone(fromZone);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("dateTime must be ISO-8601, for example 2026-04-05T15:30:00");
        }
    }

    private ZoneId resolveZoneId(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        try {
            return ZoneId.of(normalized);
        } catch (Exception exception) {
            throw new IllegalArgumentException(fieldName + " must be a valid IANA time zone");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
