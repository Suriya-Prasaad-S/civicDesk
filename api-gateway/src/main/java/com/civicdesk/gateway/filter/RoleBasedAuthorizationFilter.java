package com.civicdesk.gateway.filter;

import com.civicdesk.gateway.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Role-Based Authorization Filter - Enforces access control based on user roles.
 * Complements JWT authentication with fine-grained authorization.
 */
@Component
public class RoleBasedAuthorizationFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(RoleBasedAuthorizationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RoleBasedAuthorizationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // Define path-to-roles mapping for authorization
    private static final Map<String, List<String>> ROLE_RESTRICTED_PATHS = new HashMap<>();

    static {
        // Admin-only endpoints
        ROLE_RESTRICTED_PATHS.put("/civicDesk/iam/auth/admin/**", List.of("ADMIN"));
        ROLE_RESTRICTED_PATHS.put("/civicDesk/analytics/**", List.of("ADMIN", "STAFF"));

        // Staff-only endpoints
        ROLE_RESTRICTED_PATHS.put("/civicDesk/permits/**", List.of("STAFF", "ADMIN"));
        ROLE_RESTRICTED_PATHS.put("/civicDesk/workorders/**", List.of("STAFF", "ADMIN"));
        ROLE_RESTRICTED_PATHS.put("/civicDesk/grievance/**", List.of("STAFF", "ADMIN"));

        // Citizen endpoints
        ROLE_RESTRICTED_PATHS.put("/civicDesk/citizens/**", List.of("CITIZEN", "STAFF", "ADMIN"));
        ROLE_RESTRICTED_PATHS.put("/civicDesk/serviceRequest/**", List.of("CITIZEN", "STAFF", "ADMIN"));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Check if path requires specific roles
        String requiredRoles = findRequiredRoles(path);
        if (requiredRoles == null) {
            // No specific role requirement, proceed
            return chain.filter(exchange);
        }

        // Extract JWT token
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Access denied: Missing authorization header for protected path: {}", path);
            return forbiddenResponse(exchange, "Authorization required for this endpoint");
        }

        String token = authHeader.substring(7);
        
        try {
            // Get claims and validate role
            Claims claims = jwtTokenProvider.getClaims(token);
            String userRole = claims.get("role", String.class);
            
            if (userRole == null || !requiredRoles.contains(userRole)) {
                log.warn("Access denied: User role '{}' not authorized for path: {}", userRole, path);
                return forbiddenResponse(exchange, 
                        String.format("Your role '%s' is not authorized for this endpoint", userRole));
            }

            // Role is authorized, add user info to headers for downstream services
            String userId = claims.getSubject();
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-ID", userId)
                    .header("X-User-Role", userRole)
                    .header("X-User-Authorized", "true")
                    .build();

            log.debug("Authorization granted: user={} role={} path={}", userId, userRole, path);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.error("Authorization filter error: {}", e.getMessage());
            return forbiddenResponse(exchange, "Authorization validation failed");
        }
    }

    @Override
    public int getOrder() {
        return 0; // Run after JWT filter
    }

    private String findRequiredRoles(String path) {
        for (Map.Entry<String, List<String>> entry : ROLE_RESTRICTED_PATHS.entrySet()) {
            if (pathMatcher.match(entry.getKey(), path)) {
                return String.join(",", entry.getValue());
            }
        }
        return null;
    }

    private Mono<Void> forbiddenResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json");

        String errorJson = String.format("{\"error\": \"%s\", \"status\": 403}", message);
        byte[] bytes = errorJson.getBytes();

        return response.writeWith(
            Mono.fromCallable(() -> response.bufferFactory().wrap(bytes))
        );
    }
}
