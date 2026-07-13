package com.civicdesk.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Enhanced Logging Filter with Request Correlation & Trace ID Support.
 * Logs all requests/responses with trace IDs and correlation IDs for observability.
 */
@Component
public class EnhancedLoggingFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(EnhancedLoggingFilter.class);

    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // Generate IDs for tracing
        String traceId = extractOrGenerateTraceId(request);
        String correlationId = extractOrGenerateCorrelationId(request);
        String requestId = UUID.randomUUID().toString();

        // Add headers for downstream services
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(TRACE_ID_HEADER, traceId)
                .header(CORRELATION_ID_HEADER, correlationId)
                .header(REQUEST_ID_HEADER, requestId)
                .header("X-Forwarded-By", "api-gateway")
                .build();

        // Add response headers for client
        response.getHeaders().add(TRACE_ID_HEADER, traceId);
        response.getHeaders().add(REQUEST_ID_HEADER, requestId);

        long startTime = System.currentTimeMillis();
        String path = request.getPath().value();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String clientIp = extractClientIp(request);

        log.info("GATEWAY_REQUEST TRACE={} CORRELATION={} REQUEST={} {} {} CLIENT={} USER_AGENT={}",
                traceId, correlationId, requestId, method, path, clientIp,
                request.getHeaders().getFirst(HttpHeaders.USER_AGENT));

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .doFinally(signal -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int statusCode = response.getStatusCode() != null ? 
                            response.getStatusCode().value() : 0;

                    log.info("GATEWAY_RESPONSE TRACE={} CORRELATION={} REQUEST={} {} {} STATUS={} DURATION={}ms",
                            traceId, correlationId, requestId, method, path, statusCode, duration);

                    if (duration > 5000) {
                        log.warn("SLOW_REQUEST TRACE={} CORRELATION={} REQUEST={} {} {} DURATION={}ms",
                                traceId, correlationId, requestId, method, path, duration);
                    }

                    if (statusCode >= 500) {
                        log.error("SERVER_ERROR TRACE={} CORRELATION={} REQUEST={} {} {} STATUS={}",
                                traceId, correlationId, requestId, method, path, statusCode);
                    }
                });
    }

    @Override
    public int getOrder() {
        return -3; // Run before all other filters
    }

    private String extractOrGenerateTraceId(ServerHttpRequest request) {
        String headerTraceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        return headerTraceId != null && !headerTraceId.isEmpty() ? 
                headerTraceId : UUID.randomUUID().toString();
    }

    private String extractOrGenerateCorrelationId(ServerHttpRequest request) {
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        return correlationId != null && !correlationId.isEmpty() ? 
                correlationId : UUID.randomUUID().toString();
    }

    private String extractClientIp(ServerHttpRequest request) {
        // Check for X-Forwarded-For header first (behind proxy)
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }

        // Fall back to remote address
        var remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return "unknown";
    }
}
