package io.quintkard.quintkardapp.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HttpRequestLoggingFilterTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void usesRequestHeaderAndForwardedIpAndCleansUpMdc() throws Exception {
        TestableHttpRequestLoggingFilter filter = new TestableHttpRequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/messages");
        request.addHeader("X-Request-Id", "  req-123  ");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestId = new AtomicReference<>();
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> clientIp = new AtomicReference<>();

        filter.invoke(request, response, (req, res) -> {
            requestId.set(MDC.get("requestId"));
            method.set(MDC.get("httpMethod"));
            path.set(MDC.get("httpPath"));
            clientIp.set(MDC.get("clientIp"));
        });

        assertEquals("req-123", response.getHeader("X-Request-Id"));
        assertEquals("req-123", requestId.get());
        assertEquals("GET", method.get());
        assertEquals("/api/messages", path.get());
        assertEquals("1.2.3.4", clientIp.get());
        assertNull(MDC.get("requestId"));
    }

    @Test
    void generatesRequestIdAndUsesRemoteAddressWhenHeadersMissing() throws Exception {
        TestableHttpRequestLoggingFilter filter = new TestableHttpRequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/cards");
        request.setRemoteAddr("9.8.7.6");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestId = new AtomicReference<>();
        AtomicReference<String> clientIp = new AtomicReference<>();

        filter.invoke(request, response, (req, res) -> {
            requestId.set(MDC.get("requestId"));
            clientIp.set(MDC.get("clientIp"));
        });

        assertNotNull(response.getHeader("X-Request-Id"));
        assertEquals(response.getHeader("X-Request-Id"), requestId.get());
        assertEquals("9.8.7.6", clientIp.get());
    }

    @Test
    void treatsBlankHeadersAsMissing() throws Exception {
        TestableHttpRequestLoggingFilter filter = new TestableHttpRequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/cards");
        request.addHeader("X-Request-Id", "   ");
        request.addHeader("X-Forwarded-For", "   ");
        request.setRemoteAddr("9.8.7.6");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestId = new AtomicReference<>();
        AtomicReference<String> clientIp = new AtomicReference<>();

        filter.invoke(request, response, (req, res) -> {
            requestId.set(MDC.get("requestId"));
            clientIp.set(MDC.get("clientIp"));
        });

        assertNotNull(response.getHeader("X-Request-Id"));
        assertEquals(response.getHeader("X-Request-Id"), requestId.get());
        assertEquals("9.8.7.6", clientIp.get());
    }

    @Test
    void rethrowsServletExceptionFromFilterChain() {
        TestableHttpRequestLoggingFilter filter = new TestableHttpRequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/messages");
        MockHttpServletResponse response = new MockHttpServletResponse();

        ServletException exception = assertThrows(
                ServletException.class,
                () -> filter.invoke(request, response, (req, res) -> {
                    throw new ServletException("boom");
                })
        );

        assertEquals("boom", exception.getMessage());
    }

    @Test
    void rethrowsIoExceptionFromFilterChain() {
        TestableHttpRequestLoggingFilter filter = new TestableHttpRequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/messages");
        MockHttpServletResponse response = new MockHttpServletResponse();

        IOException exception = assertThrows(
                IOException.class,
                () -> filter.invoke(request, response, (req, res) -> {
                    throw new IOException("boom");
                })
        );

        assertEquals("boom", exception.getMessage());
    }

    @Test
    void rethrowsRuntimeExceptionFromFilterChain() {
        TestableHttpRequestLoggingFilter filter = new TestableHttpRequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/messages");
        MockHttpServletResponse response = new MockHttpServletResponse();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> filter.invoke(request, response, (req, res) -> {
                    throw new IllegalStateException("boom");
                })
        );

        assertEquals("boom", exception.getMessage());
    }

    private static final class TestableHttpRequestLoggingFilter extends HttpRequestLoggingFilter {
        private void invoke(MockHttpServletRequest request, MockHttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            doFilterInternal(request, response, filterChain);
        }
    }
}
