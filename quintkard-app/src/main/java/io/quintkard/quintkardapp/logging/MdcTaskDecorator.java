package io.quintkard.quintkardapp.logging;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previousContext = MDC.getCopyOfContextMap();
            try {
                if (contextMap == null || contextMap.isEmpty()) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                if (previousContext == null || previousContext.isEmpty()) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(previousContext);
                }
            }
        };
    }
}
