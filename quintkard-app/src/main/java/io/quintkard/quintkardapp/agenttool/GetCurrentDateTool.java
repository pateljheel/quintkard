package io.quintkard.quintkardapp.agenttool;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class GetCurrentDateTool implements AiTool {

    private final ObjectMapper objectMapper;

    public GetCurrentDateTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "get_current_date";
    }

    @Override
    public String description() {
        return "Get the current date. Optional timeZone must be a valid IANA zone like America/New_York or UTC.";
    }

    @Override
    public Class<?> inputType() {
        return GetCurrentDateArgs.class;
    }

    @Override
    public Object execute(AiToolExecutionRequest request) {
        GetCurrentDateArgs arguments = objectMapper.convertValue(request.arguments(), GetCurrentDateArgs.class);
        ZoneId zoneId = resolveZoneId(arguments.timeZone());
        LocalDate today = LocalDate.now(zoneId);
        return Map.of(
                "timeZone", zoneId.getId(),
                "date", today.toString()
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
