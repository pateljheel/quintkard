package io.quintkard.quintkardapp.agenttool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class UtilityToolsTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void getCurrentTimeUsesRequestedTimeZone() {
        GetCurrentTimeTool tool = new GetCurrentTimeTool(objectMapper);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.execute(new AiToolExecutionRequest(
                "admin",
                "conv-1",
                Map.of("timeZone", "UTC")
        ));

        assertEquals("UTC", result.get("timeZone"));
        assertNotNull(result.get("instant"));
        assertNotNull(result.get("offsetDateTime"));
    }

    @Test
    void getCurrentTimeExposesMetadata() {
        GetCurrentTimeTool tool = new GetCurrentTimeTool(objectMapper);

        assertEquals("get_current_time", tool.name());
        assertTrue(tool.description().contains("valid IANA zone"));
        assertSame(GetCurrentTimeArgs.class, tool.inputType());
    }

    @Test
    void getCurrentTimeUsesSystemDefaultWhenTimeZoneMissing() {
        GetCurrentTimeTool tool = new GetCurrentTimeTool(objectMapper);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.execute(new AiToolExecutionRequest(
                "admin",
                "conv-1",
                Map.of()
        ));

        assertEquals(java.time.ZoneId.systemDefault().getId(), result.get("timeZone"));
    }

    @Test
    void getCurrentTimeUsesSystemDefaultWhenTimeZoneBlank() {
        GetCurrentTimeTool tool = new GetCurrentTimeTool(objectMapper);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.execute(new AiToolExecutionRequest(
                "admin",
                "conv-1",
                Map.of("timeZone", "   ")
        ));

        assertEquals(java.time.ZoneId.systemDefault().getId(), result.get("timeZone"));
    }

    @Test
    void getCurrentTimeRejectsInvalidTimeZone() {
        GetCurrentTimeTool tool = new GetCurrentTimeTool(objectMapper);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tool.execute(new AiToolExecutionRequest(
                        "admin",
                        "conv-1",
                        Map.of("timeZone", "Mars/Olympus")
                ))
        );

        assertEquals("timeZone must be a valid IANA time zone", exception.getMessage());
    }

    @Test
    void getCurrentDateRejectsInvalidTimeZone() {
        GetCurrentDateTool tool = new GetCurrentDateTool(objectMapper);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tool.execute(new AiToolExecutionRequest(
                        "admin",
                        "conv-1",
                        Map.of("timeZone", "Mars/Olympus")
                ))
        );

        assertEquals("timeZone must be a valid IANA time zone", exception.getMessage());
    }

    @Test
    void getCurrentDateUsesRequestedTimeZone() {
        GetCurrentDateTool tool = new GetCurrentDateTool(objectMapper);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.execute(new AiToolExecutionRequest(
                "admin",
                "conv-1",
                Map.of("timeZone", "UTC")
        ));

        assertEquals("UTC", result.get("timeZone"));
        assertNotNull(result.get("date"));
    }

    @Test
    void getCurrentDateExposesMetadata() {
        GetCurrentDateTool tool = new GetCurrentDateTool(objectMapper);

        assertEquals("get_current_date", tool.name());
        assertTrue(tool.description().contains("valid IANA zone"));
        assertSame(GetCurrentDateArgs.class, tool.inputType());
    }

    @Test
    void getCurrentDateUsesSystemDefaultWhenTimeZoneMissing() {
        GetCurrentDateTool tool = new GetCurrentDateTool(objectMapper);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.execute(new AiToolExecutionRequest(
                "admin",
                "conv-1",
                Map.of()
        ));

        assertEquals(java.time.ZoneId.systemDefault().getId(), result.get("timeZone"));
        assertNotNull(result.get("date"));
    }

    @Test
    void getCurrentDateUsesSystemDefaultWhenTimeZoneBlank() {
        GetCurrentDateTool tool = new GetCurrentDateTool(objectMapper);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.execute(new AiToolExecutionRequest(
                "admin",
                "conv-1",
                Map.of("timeZone", "   ")
        ));

        assertEquals(java.time.ZoneId.systemDefault().getId(), result.get("timeZone"));
        assertNotNull(result.get("date"));
    }

    @Test
    void convertTimeZoneConvertsIsoDateTimeAcrossZones() {
        ConvertTimeZoneTool tool = new ConvertTimeZoneTool(objectMapper);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.execute(new AiToolExecutionRequest(
                "admin",
                "conv-1",
                Map.of(
                        "dateTime", "2026-04-05T15:30:00",
                        "fromTimeZone", "UTC",
                        "toTimeZone", "America/New_York"
                )
        ));

        assertEquals("UTC", result.get("sourceTimeZone"));
        assertEquals("America/New_York", result.get("targetTimeZone"));
        assertTrue(String.valueOf(result.get("convertedDateTime")).contains("America/New_York"));
        assertTrue(String.valueOf(result.get("convertedOffsetDateTime")).contains("-04:00"));
    }

    @Test
    void convertTimeZoneExposesMetadata() {
        ConvertTimeZoneTool tool = new ConvertTimeZoneTool(objectMapper);

        assertEquals("convert_time_zone", tool.name());
        assertTrue(tool.description().contains("ISO-8601"));
        assertSame(ConvertTimeZoneArgs.class, tool.inputType());
    }

    @Test
    void convertTimeZoneConvertsOffsetDateTimeInput() {
        ConvertTimeZoneTool tool = new ConvertTimeZoneTool(objectMapper);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.execute(new AiToolExecutionRequest(
                "admin",
                "conv-1",
                Map.of(
                        "dateTime", "2026-04-05T15:30:00Z",
                        "fromTimeZone", "UTC",
                        "toTimeZone", "America/New_York"
                )
        ));

        assertEquals("UTC", result.get("sourceTimeZone"));
        assertEquals("America/New_York", result.get("targetTimeZone"));
        assertTrue(String.valueOf(result.get("sourceDateTime")).contains("Z"));
    }

    @Test
    void convertTimeZoneRejectsInvalidDateFormat() {
        ConvertTimeZoneTool tool = new ConvertTimeZoneTool(objectMapper);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tool.execute(new AiToolExecutionRequest(
                        "admin",
                        "conv-1",
                        Map.of(
                                "dateTime", "29th May",
                                "fromTimeZone", "UTC",
                                "toTimeZone", "America/New_York"
                        )
                ))
        );

        assertEquals("dateTime must be ISO-8601, for example 2026-04-05T15:30:00", exception.getMessage());
    }

    @Test
    void convertTimeZoneRejectsMissingDateTime() {
        ConvertTimeZoneTool tool = new ConvertTimeZoneTool(objectMapper);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tool.execute(new AiToolExecutionRequest(
                        "admin",
                        "conv-1",
                        Map.of(
                                "dateTime", "   ",
                                "fromTimeZone", "UTC",
                                "toTimeZone", "America/New_York"
                        )
                ))
        );

        assertEquals("dateTime is required", exception.getMessage());
    }

    @Test
    void convertTimeZoneRejectsMissingFromTimeZone() {
        ConvertTimeZoneTool tool = new ConvertTimeZoneTool(objectMapper);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tool.execute(new AiToolExecutionRequest(
                        "admin",
                        "conv-1",
                        Map.of(
                                "dateTime", "2026-04-05T15:30:00",
                                "toTimeZone", "America/New_York"
                        )
                ))
        );

        assertEquals("fromTimeZone is required", exception.getMessage());
    }

    @Test
    void convertTimeZoneRejectsInvalidTargetTimeZone() {
        ConvertTimeZoneTool tool = new ConvertTimeZoneTool(objectMapper);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tool.execute(new AiToolExecutionRequest(
                        "admin",
                        "conv-1",
                        Map.of(
                                "dateTime", "2026-04-05T15:30:00",
                                "fromTimeZone", "UTC",
                                "toTimeZone", "Mars/Olympus"
                        )
                ))
        );

        assertEquals("toTimeZone must be a valid IANA time zone", exception.getMessage());
    }
}
