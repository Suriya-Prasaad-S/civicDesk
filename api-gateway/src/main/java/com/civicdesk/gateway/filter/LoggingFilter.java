package com.civicdesk.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        log.info("Gateway -> {} {} [from {}]",
                request.getMethod(),
                request.getPath().value(),
                request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress() : "unknown");

        long start = System.currentTimeMillis();
        return chain.filter(exchange).doFinally(signal ->
                log.info("Gateway <- {} {} {}ms",
                        exchange.getResponse().getStatusCode(),
                        request.getPath().value(),
                        System.currentTimeMillis() - start));
    }

    @Override
    public int getOrder() {
        return -2; // Run before JWT filter
    }
}
