package com.civicdesk.notification.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.service.token:}")
    private String serviceToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (token != null && jwtTokenProvider.validateToken(token)) {
                String userIdStr = jwtTokenProvider.getEmailFromToken(token);
                String role   = jwtTokenProvider.getRoleFromToken(token);
                Long   userId = jwtTokenProvider.getUserIdFromToken(token);
                JwtUserContext.set(userId, userIdStr, role);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(userIdStr, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
            } else if (serviceToken != null && !serviceToken.isBlank()) {
                String serviceHeader = request.getHeader("X-Service-Token");
                if (serviceToken.equals(serviceHeader)) {
                    JwtUserContext.set(0L, "service", "ADM");
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken("service", null,
                                    List.of(new SimpleGrantedAuthority("ROLE_ADM"))));
                }
            }
        } catch (Exception e) {
            log.error("JWT filter error: {}", e.getMessage());
        } finally {
            filterChain.doFilter(request, response);
            JwtUserContext.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return (StringUtils.hasText(header) && header.startsWith("Bearer "))
                ? header.substring(7) : null;
    }
}
