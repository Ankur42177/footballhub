package com.footballhub.security;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.footballhub.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fixed-window, per-client-IP rate limiting for authentication attempts and for
 * state-changing API calls (comments, votes, posts, reports). In-memory, so limits apply
 * per instance; move this to Redis if the API is scaled horizontally.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_KEYS = 50_000;

    private final AppProperties properties;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        AppProperties.RateLimit limits = properties.rateLimit();
        String bucket = limits.enabled() ? bucketFor(request) : null;
        if (bucket == null) {
            chain.doFilter(request, response);
            return;
        }
        int limit = "auth".equals(bucket) ? limits.authPerMinute() : limits.writesPerMinute();
        long minute = System.currentTimeMillis() / 60_000L;
        String key = bucket + '|' + request.getRemoteAddr();

        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.entrySet().removeIf(entry -> entry.getValue().minute != minute);
        }
        Window window = windows.compute(key,
                (k, existing) -> existing == null || existing.minute != minute ? new Window(minute) : existing);

        if (window.count.incrementAndGet() > limit) {
            long secondsLeft = 60 - (System.currentTimeMillis() / 1000L) % 60;
            response.setHeader("Retry-After", Long.toString(secondsLeft));
            JsonSecurityHandlers.write(response, 429, "RATE_LIMITED",
                    "Too many requests. Please wait a moment and try again.");
            return;
        }
        chain.doFilter(request, response);
    }

    private static String bucketFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (path == null || !path.startsWith("/api/v1/")) {
            return null;
        }
        boolean mutating = !("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method));
        if (!mutating) {
            return null;
        }
        if (path.startsWith("/api/v1/auth/") && !path.equals("/api/v1/auth/logout")) {
            return "auth";
        }
        return "write";
    }

    private static final class Window {
        private final long minute;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long minute) {
            this.minute = minute;
        }
    }
}
