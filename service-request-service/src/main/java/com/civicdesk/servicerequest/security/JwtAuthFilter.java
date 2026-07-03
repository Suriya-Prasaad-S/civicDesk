package com.civicdesk.servicerequest.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                String userIdStr = jwtTokenProvider.getEmailFromToken(jwt);
                String role  = jwtTokenProvider.getRoleFromToken(jwt);
                Long userId  = jwtTokenProvider.getUserIdFromToken(jwt);

                JwtUserContext.set(userId, userIdStr, role);

                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                userIdStr, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));

                log.debug("JWT auth: userId={} role={}", userIdStr, role);
            }
        } catch (Exception e) {
            log.error("JWT auth failed: {}", e.getMessage());
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            JwtUserContext.clear();
        }
    }

    private String parseJwt(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return (StringUtils.hasText(header) && header.startsWith("Bearer "))
                ? header.substring(7) : null;
    }
}
