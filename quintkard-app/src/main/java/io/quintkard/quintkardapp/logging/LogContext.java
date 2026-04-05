package io.quintkard.quintkardapp.logging;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;

public final class LogContext {

    private LogContext() {
    }

    public static AutoCloseable with(String key, String value) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(key, value);
        return with(values);
    }

    public static AutoCloseable with(Map<String, String> values) {
        Map<String, String> previousValues = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            previousValues.put(key, MDC.get(key));

            String value = entry.getValue();
            if (value == null || value.isBlank()) {
                MDC.remove(key);
            } else {
                MDC.put(key, value);
            }
        }

        return () -> {
            for (Map.Entry<String, String> entry : previousValues.entrySet()) {
                if (entry.getValue() == null) {
                    MDC.remove(entry.getKey());
                } else {
                    MDC.put(entry.getKey(), entry.getValue());
                }
            }
        };
    }
}
