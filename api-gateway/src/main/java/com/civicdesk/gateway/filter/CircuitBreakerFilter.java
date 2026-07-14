package com.civicdesk.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.time.Duration;

/**
 * Circuit Breaker Filter - Placeholder implementation.
 * Resilience4j circuit breaker pattern is managed at the framework level.
 * 
 * This filter can be extended to implement:
 * - Timeout policies
 * - Retry logic
 * - Fallback responses
 */
@Component
public class CircuitBreakerFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerFilter.class);

    private static final long REQUEST_TIMEOUT_MS = 10000; // 10 seconds

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // Pass through with timeout logic handled by Spring Cloud Gateway
        return chain.filter(exchange)
                .timeout(java.time.Duration.ofMillis(REQUEST_TIMEOUT_MS))
                .onErrorResume(throwable -> {
                    log.warn("Request timeout for path: {}", path);
                    return handleTimeout(exchange);
                });
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private Mono<Void> handleTimeout(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().add("Retry-After", "30");

        String message = "{\"error\": \"Request timeout. Please try again later.\", \"status\": 503}";
        byte[] bytes = message.getBytes();

        return response.writeWith(
            Mono.fromCallable(() -> response.bufferFactory().wrap(bytes))
        );
    }
}
