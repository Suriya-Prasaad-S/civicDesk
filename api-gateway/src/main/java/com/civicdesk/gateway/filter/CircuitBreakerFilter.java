package com.civicdesk.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
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

    @Value("${gateway.circuit-breaker.request-timeout-ms:10000}")
    private long requestTimeoutMs;

    @Value("${gateway.circuit-breaker.retry-after-sec:30}")
    private int retryAfterSeconds;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        return chain.filter(exchange)
                .timeout(Duration.ofMillis(requestTimeoutMs))
                .onErrorResume(throwable -> {
                    log.warn("Request timeout/exception for path: {} -> {}: {}", path, throwable.getClass().getSimpleName(), throwable.getMessage());
                    log.debug("Full stack for timeout/exception on path {}", path, throwable);
                    return handleTimeout(exchange, throwable);
                });
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private Mono<Void> handleTimeout(ServerWebExchange exchange, Throwable throwable) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().add("Retry-After", String.valueOf(retryAfterSeconds));

        String message = String.format("{\"error\": \"Request failed: %s\", \"status\": 503}",
                throwable == null ? "Request timeout" : throwable.getClass().getSimpleName());
        byte[] bytes = message.getBytes();

        return response.writeWith(
                Mono.fromCallable(() -> response.bufferFactory().wrap(bytes))
        );
    }
}
