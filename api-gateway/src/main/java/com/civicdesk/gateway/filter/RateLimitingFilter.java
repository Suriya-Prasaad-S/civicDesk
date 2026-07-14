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

/**
 * Rate Limiting Filter - Placeholder implementation.
 * Full distributed rate limiting with Redis can be added in future versions.
 * 
 * This filter provides the foundation for request rate limiting.
 */
@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final int MAX_REQUESTS_PER_MINUTE = 600;
    private int requestCount = 0;
    private long lastResetTime = System.currentTimeMillis();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientId = extractClientIdentifier(exchange.getRequest());
        String path = exchange.getRequest().getPath().value();

        // Simple in-memory rate limiting (reset every minute)
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastResetTime > 60000) {
            requestCount = 0;
            lastResetTime = currentTime;
        }

        if (requestCount >= MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for client: {} on path: {}", clientId, path);
            return rateLimitExceeded(exchange);
        }

        requestCount++;
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
}
