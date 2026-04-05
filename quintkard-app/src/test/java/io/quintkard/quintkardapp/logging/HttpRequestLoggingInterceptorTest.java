package io.quintkard.quintkardapp.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class HttpRequestLoggingInterceptorTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void preHandleStoresRequestStartTimestamp() {
        HttpRequestLoggingInterceptor interceptor = new HttpRequestLoggingInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/messages");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertTrue(request.getAttribute("io.quintkard.quintkardapp.logging.HttpRequestLoggingInterceptor.startedAt") instanceof Long);
    }

    @Test
    void afterCompletionUsesAuthenticatedUserAndCleansMdc() {
        HttpRequestLoggingInterceptor interceptor = new HttpRequestLoggingInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/messages");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        request.setAttribute("io.quintkard.quintkardapp.logging.HttpRequestLoggingInterceptor.startedAt", System.currentTimeMillis() - 10);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "n/a")
        );

        interceptor.afterCompletion(request, response, new Object(), null);

        assertNull(MDC.get("userId"));
    }

    @Test
    void afterCompletionUsesCurrentTimeWhenStartedAtMissingForAuthenticatedUser() {
        HttpRequestLoggingInterceptor interceptor = new HttpRequestLoggingInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/messages");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(204);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "n/a")
        );

        interceptor.afterCompletion(request, response, new Object(), null);

        assertNull(MDC.get("userId"));
    }

    @Test
    void afterCompletionIgnoresAnonymousUserAndMissingStartTime() {
        HttpRequestLoggingInterceptor interceptor = new HttpRequestLoggingInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/messages");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", "n/a")
        );

        interceptor.afterCompletion(request, response, new Object(), null);

        assertNull(MDC.get("userId"));
    }

    @Test
    void afterCompletionIgnoresMissingAuthentication() {
        HttpRequestLoggingInterceptor interceptor = new HttpRequestLoggingInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/messages");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.afterCompletion(request, response, new Object(), null);

        assertNull(MDC.get("userId"));
    }

    @Test
    void afterCompletionIgnoresUnauthenticatedUser() {
        HttpRequestLoggingInterceptor interceptor = new HttpRequestLoggingInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/messages");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(authentication.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        interceptor.afterCompletion(request, response, new Object(), null);

        assertNull(MDC.get("userId"));
    }

    @Test
    void afterCompletionIgnoresAuthenticationWithNullName() {
        HttpRequestLoggingInterceptor interceptor = new HttpRequestLoggingInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/messages");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(authentication.isAuthenticated()).thenReturn(true);
        org.mockito.Mockito.when(authentication.getName()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        interceptor.afterCompletion(request, response, new Object(), null);

        assertNull(MDC.get("userId"));
    }

    @Test
    void afterCompletionHandlesFailurePath() {
        HttpRequestLoggingInterceptor interceptor = new HttpRequestLoggingInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/messages");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);
        request.setAttribute("io.quintkard.quintkardapp.logging.HttpRequestLoggingInterceptor.startedAt", 1L);

        interceptor.afterCompletion(request, response, new Object(), new IllegalStateException("boom"));

        assertNull(MDC.get("userId"));
        assertEquals(500, response.getStatus());
    }

    @Test
    void afterCompletionHandlesFailurePathForAuthenticatedUser() {
        HttpRequestLoggingInterceptor interceptor = new HttpRequestLoggingInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/messages");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);
        request.setAttribute("io.quintkard.quintkardapp.logging.HttpRequestLoggingInterceptor.startedAt", 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "n/a")
        );

        interceptor.afterCompletion(request, response, new Object(), new IllegalStateException("boom"));

        assertNull(MDC.get("userId"));
    }
}
