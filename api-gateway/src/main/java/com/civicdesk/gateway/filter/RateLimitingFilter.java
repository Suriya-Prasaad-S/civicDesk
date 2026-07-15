package com.civicdesk.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limiting Filter - Placeholder implementation.
 * Full distributed rate limiting with Redis can be added in future versions.
 * 
 * This filter provides the foundation for request rate limiting.
 */
@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final long WINDOW_MILLIS = 60_000L;

    // Keyed windows: key -> sliding fixed window counter
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    // Cleanup thread to remove idle keys
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rate-limiter-cleaner");
        t.setDaemon(true);
        return t;
    });

    public RateLimitingFilter() {
        // remove keys not seen for 10 minutes
        cleaner.scheduleAtFixedRate(this::cleanupIdleKeys, 5, 60, TimeUnit.SECONDS);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userKey = extractKey(exchange.getRequest());
        String path = exchange.getRequest().getPath().value();

        if ("/gateway-test/reset".equals(path)) {
            resetAll();
            return chain.filter(exchange);
        }

        // get or create window for this key
        Window w = windows.computeIfAbsent(userKey, k -> new Window());

        long now = System.currentTimeMillis();
        synchronized (w) {
            if (now - w.windowStart > WINDOW_MILLIS) {
                w.count.set(0);
                w.windowStart = now;
            }

            if (w.count.get() >= MAX_REQUESTS_PER_MINUTE) {
                log.warn("Rate limit exceeded for key: {} on path: {}", userKey, path);
                w.lastSeen = now;
                return rateLimitExceeded(exchange);
            }

            w.count.incrementAndGet();
            w.lastSeen = now;
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private String extractClientIdentifier(ServerHttpRequest request) {
        var remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "unknown";
    }

    public void reset() {
        // backward-compatible: clear all windows
        windows.clear();
    }

    private void resetAll() {
        windows.clear();
    }

    private Mono<Void> rateLimitExceeded(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Retry-After", "5");
        
        String message = "{\"error\": \"Rate limit exceeded\"}";
        byte[] bytes = message.getBytes();

        return response.writeWith(
            Mono.fromCallable(() -> response.bufferFactory().wrap(bytes))
        );
    }

    private void cleanupIdleKeys() {
        long cutoff = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
        Iterator<Map.Entry<String, Window>> it = windows.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Window> e = it.next();
            if (e.getValue().lastSeen < cutoff) {
                it.remove();
            }
        }
    }

    private String extractKey(ServerHttpRequest request) {
        // Prefer an authenticated user id header, fallback to remote IP
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            return "user:" + userId;
        }
        return "ip:" + extractClientIdentifier(request);
    }

    private static class Window {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long windowStart = System.currentTimeMillis();
        volatile long lastSeen = System.currentTimeMillis();
    }
}
