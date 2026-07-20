package com.civicdesk.analytics.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Puts a per-request correlation id into the SLF4J {@link MDC} under {@code traceId}, so every log
 * line for one request can be tied together (rendered by {@code [%X{traceId}]} in logback-spring.xml).
 *
 * <p>If an upstream caller (e.g. the API gateway) already sent an {@code X-Correlation-Id} header,
 * that value is reused so the trace spans services; otherwise a fresh short id is generated. The id
 * is echoed back on the response and always cleared in a {@code finally} block (thread-pool safety).
 *
 * <p>Runs at highest precedence so the id is present before any other filter (security, etc.) logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(CORRELATION_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put(TRACE_ID, traceId);
        response.setHeader(CORRELATION_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }
}
