package io.quintkard.quintkardapp.agenttool;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class GetCurrentTimeTool implements AiTool {

    private final ObjectMapper objectMapper;

    public GetCurrentTimeTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "get_current_time";
    }

    @Override
    public String description() {
        return "Get the current exact time. Optional timeZone must be a valid IANA zone like America/New_York or UTC.";
    }

    @Override
    public Class<?> inputType() {
        return GetCurrentTimeArgs.class;
    }

    @Override
    public Object execute(AiToolExecutionRequest request) {
        GetCurrentTimeArgs arguments = objectMapper.convertValue(request.arguments(), GetCurrentTimeArgs.class);

        ZoneId zoneId = resolveZoneId(arguments.timeZone());
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        return Map.of(
                "timeZone", zoneId.getId(),
                "instant", Instant.now().toString(),
                "localDateTime", now.toLocalDateTime().toString(),
                "offsetDateTime", now.toOffsetDateTime().toString(),
                "formatted", now.toString()
        );
    }

    private ZoneId resolveZoneId(String timeZone) {
        String normalized = timeZone == null ? null : timeZone.trim();
        if (normalized == null || normalized.isEmpty()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(normalized);
        } catch (Exception exception) {
            throw new IllegalArgumentException("timeZone must be a valid IANA time zone");
        }
    }
}
