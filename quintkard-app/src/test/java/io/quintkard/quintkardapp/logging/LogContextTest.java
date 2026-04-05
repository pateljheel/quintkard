package io.quintkard.quintkardapp.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class LogContextTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void withSingleKeySetsAndRestoresPreviousValue() throws Exception {
        MDC.put("userId", "before");

        try (AutoCloseable ignored = LogContext.with("userId", "after")) {
            assertEquals("after", MDC.get("userId"));
        }

        assertEquals("before", MDC.get("userId"));
    }

    @Test
    void withBlankValueRemovesKeyAndRestoresPreviousValue() throws Exception {
        MDC.put("userId", "before");

        try (AutoCloseable ignored = LogContext.with("userId", "   ")) {
            assertNull(MDC.get("userId"));
        }

        assertEquals("before", MDC.get("userId"));
    }

    @Test
    void withNullValueRemovesKeyWithoutPreviousValue() throws Exception {
        try (AutoCloseable ignored = LogContext.with("userId", null)) {
            assertNull(MDC.get("userId"));
        }

        assertNull(MDC.get("userId"));
    }

    @Test
    void withMapSetsMultipleKeysAndRestoresMissingEntries() throws Exception {
        MDC.put("requestId", "old-request");

        try (AutoCloseable ignored = LogContext.with(Map.of(
                "requestId", "new-request",
                "messageId", "msg-1"
        ))) {
            assertEquals("new-request", MDC.get("requestId"));
            assertEquals("msg-1", MDC.get("messageId"));
        }

        assertEquals("old-request", MDC.get("requestId"));
        assertNull(MDC.get("messageId"));
    }
}
