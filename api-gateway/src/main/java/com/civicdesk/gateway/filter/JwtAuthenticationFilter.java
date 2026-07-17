package com.civicdesk.gateway.filter;

import com.civicdesk.gateway.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;

    // Paths that are accessible without a JWT token
    private static final List<String> PUBLIC_PATHS = List.of(
            "/civicDesk/iam/auth/register",
            "/civicDesk/iam/auth/citizen/login",
            "/civicDesk/iam/auth/staff/login",
            "/civicDesk/iam/auth/setPassword",
            "/civicDesk/citizenProfile/register",
            "/civicDesk/workorders/public/**",
            "/civicDesk/serviceRequest/getAllServices",
            "/civicDesk/serviceRequest/getService/**",
            // Swagger / docs
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api-docs/**",
            "/**/api-docs",
            "/webjars/**",
            // Service docs endpoints for Swagger aggregation
            "/auth-service/api-docs",
            "/citizen-service/api-docs",
            "/service-request-service/api-docs",
            "/permit-service/api-docs",
            "/grievance-service/api-docs",
            "/public-works-service/api-docs",
            "/notification-service/api-docs",
            "/analytics-service/api-docs"
    );

        private static final List<String> PUBLIC_GET_PATHS = List.of(
            "/gateway-test/**",
            "/gateway-proxy/**"
        );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path   = request.getPath().value();
        String method = request.getMethod().name();

        // Allow public paths unconditionally
        if (isPublicPath(path) || isPublicGetPath(path, method)) {
            return chain.filter(exchange);
        }

        // Require valid Bearer token for all other requests
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            return unauthorizedResponse(exchange, "Authorization token is required.");
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("Invalid JWT token for path: {}", path);
            return unauthorizedResponse(exchange, "Invalid or expired token.");
        }

        // Forward the role as a downstream header for convenience (downstream services also validate independently)
        String role = jwtTokenProvider.getRoleFromToken(token);
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Role", role)
                .build();

        log.debug("Gateway authorized: path={} role={}", path, role);
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -1; // Run before routing filters
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream()
                .anyMatch(p -> pathMatcher.match(p, path));
    }

    private boolean isPublicGetPath(String path, String method) {
        return "GET".equalsIgnoreCase(method) && PUBLIC_GET_PATHS.stream()
                .anyMatch(p -> pathMatcher.match(p, path));
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"success\":false,\"message\":\"" + message + "\"}").getBytes();
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }
}
