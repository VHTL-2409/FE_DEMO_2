package com.example.demo.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter implements Filter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private final Cache<String, RateLimitEntry> attemptCache;

    @Value("${app.ratelimit.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.ratelimit.window-minutes:15}")
    private int windowMinutes;

    public RateLimitFilter() {
        this.attemptCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(30))
                .build();
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (!LOGIN_PATH.equals(request.getRequestURI()) || !"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        String username = extractUsernameFromBody(request);

        if (username == null || username.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        String key = buildKey(clientIp, username);
        RateLimitEntry entry = attemptCache.getIfPresent(key);

        if (entry != null && entry.isLocked(windowMinutes)) {
            log.warn("Rate limit triggered for user '{}' from IP {}. Locked until {}",
                    username, clientIp, entry.getLockExpiry());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(entry.getRetryAfterSeconds(windowMinutes)));
            response.getWriter().write(
                "{\"error\":\"TOO_MANY_ATTEMPTS\"," +
                "\"message\":\"Quá nhiều lần thử đăng nhập. Vui lòng thử lại sau " + windowMinutes + " phút.\"," +
                "\"retryAfter\":" + entry.getRetryAfterSeconds(windowMinutes) + "}"
            );
            return;
        }

        chain.doFilter(request, response);

        if (response.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
            incrementAttempt(key, username, clientIp);
        }
    }

    private synchronized void incrementAttempt(String key, String username, String clientIp) {
        RateLimitEntry entry = attemptCache.getIfPresent(key);
        if (entry == null) {
            entry = new RateLimitEntry();
        }
        entry.increment();
        attemptCache.put(key, entry);
        log.debug("Login attempt {}/{} for user '{}' from IP {}",
                entry.getCount(), maxAttempts, username, clientIp);
    }

    private String buildKey(String ip, String username) {
        return ip + "|" + username.toLowerCase().trim();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String extractUsernameFromBody(HttpServletRequest request) {
        try {
            request.setAttribute("rate-limit-body-read", true);
            String body = new String(request.getInputStream().readAllBytes());
            int usernameIdx = body.indexOf("\"username\"");
            if (usernameIdx < 0) return null;
            int colonIdx = body.indexOf(":", usernameIdx);
            int startQuote = body.indexOf("\"", colonIdx);
            int endQuote = body.indexOf("\"", startQuote + 1);
            if (colonIdx < 0 || startQuote < 0 || endQuote < 0) return null;
            return body.substring(startQuote + 1, endQuote).trim();
        } catch (Exception ex) {
            return null;
        }
    }

    private static class RateLimitEntry {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long firstAttemptMs = System.currentTimeMillis();
        private volatile long lockExpiryMs = 0;

        void increment() {
            if (count.incrementAndGet() == 1) {
                firstAttemptMs = System.currentTimeMillis();
            }
        }

        int getCount() {
            return count.get();
        }

        boolean isLocked(int windowMinutes) {
            if (lockExpiryMs > System.currentTimeMillis()) return true;
            long windowMs = windowMinutes * 60 * 1000L;
            return count.get() >= 5 &&
                   (System.currentTimeMillis() - firstAttemptMs) < windowMs;
        }

        long getLockExpiry() {
            return lockExpiryMs;
        }

        long getRetryAfterSeconds(int windowMinutes) {
            long windowMs = windowMinutes * 60 * 1000L;
            long elapsed = System.currentTimeMillis() - firstAttemptMs;
            long remaining = windowMs - elapsed;
            return Math.max(1, remaining / 1000);
        }
    }
}
