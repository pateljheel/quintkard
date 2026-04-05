package io.quintkard.quintkardapp.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MdcTaskDecoratorTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void decoratePropagatesCapturedContextAndRestoresPreviousContext() {
        MdcTaskDecorator decorator = new MdcTaskDecorator();
        MDC.put("requestId", "captured");
        Runnable decorated = decorator.decorate(() -> {
            assertEquals("captured", MDC.get("requestId"));
            MDC.put("requestId", "changed-inside");
        });

        MDC.put("requestId", "outer");
        decorated.run();

        assertEquals("outer", MDC.get("requestId"));
    }

    @Test
    void decorateClearsContextWhenNoCapturedContextExists() {
        MdcTaskDecorator decorator = new MdcTaskDecorator();
        AtomicReference<String> insideValue = new AtomicReference<>();
        Runnable decorated = decorator.decorate(() -> insideValue.set(MDC.get("requestId")));

        MDC.put("requestId", "outer");
        decorated.run();

        assertNull(insideValue.get());
        assertEquals("outer", MDC.get("requestId"));
    }

    @Test
    void decorateRestoresEmptyPreviousContextAfterRunningCapturedContext() {
        MdcTaskDecorator decorator = new MdcTaskDecorator();
        MDC.put("requestId", "captured");
        Runnable decorated = decorator.decorate(() -> assertEquals("captured", MDC.get("requestId")));

        MDC.clear();
        decorated.run();

        assertNull(MDC.get("requestId"));
    }

    @Test
    void decorateKeepsContextEmptyWhenNothingIsCapturedOrPreviouslyPresent() {
        MdcTaskDecorator decorator = new MdcTaskDecorator();
        AtomicReference<String> insideValue = new AtomicReference<>("sentinel");
        Runnable decorated = decorator.decorate(() -> insideValue.set(MDC.get("requestId")));

        MDC.clear();
        decorated.run();

        assertNull(insideValue.get());
        assertNull(MDC.get("requestId"));
    }
}
