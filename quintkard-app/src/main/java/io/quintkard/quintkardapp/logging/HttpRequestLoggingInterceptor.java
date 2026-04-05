package io.quintkard.quintkardapp.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class HttpRequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(HttpRequestLoggingInterceptor.class);
    private static final String REQUEST_STARTED_AT = HttpRequestLoggingInterceptor.class.getName() + ".startedAt";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(REQUEST_STARTED_AT, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            @Nullable Exception exception
    ) {
        long startedAt = request.getAttribute(REQUEST_STARTED_AT) instanceof Long value
                ? value
                : System.currentTimeMillis();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = null;
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getName() != null
                && !"anonymousUser".equals(authentication.getName())) {
            userId = authentication.getName();
        }

        try (AutoCloseable ignored = LogContext.with("userId", userId)) {
            if (exception == null) {
                logger.info(
                        "HTTP handler completed status={} durationMs={}",
                        response.getStatus(),
                        System.currentTimeMillis() - startedAt
                );
            } else {
                logger.error(
                        "HTTP handler failed status={} durationMs={}",
                        response.getStatus(),
                        System.currentTimeMillis() - startedAt,
                        exception
                );
            }
        } catch (Exception ignored) {
            // Ignore logging context cleanup failures.
        }
    }
}
