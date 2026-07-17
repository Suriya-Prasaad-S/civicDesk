package com.civicdesk.citizen.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Per-client Feign configuration for {@code NotificationFeignClient}.
 *
 * <p>notification-service does NOT accept the shared {@code X-Internal-Key}; for service-to-service
 * calls it trusts a static {@code X-Service-Token} header. This config attaches that token so
 * notifications can be sent even from the public self-registration flow (no end-user JWT to forward).
 *
 * <p>Deliberately NOT annotated {@code @Configuration} so it stays scoped to the notification client
 * (referenced via the client's {@code configuration} attribute) instead of leaking the token onto
 * every Feign call in the service.
 */
public class NotificationFeignConfig {

    private static final String SERVICE_TOKEN_HEADER = "X-Service-Token";

    @Value("${app.service.token}")
    private String serviceToken;

    @Bean
    public RequestInterceptor serviceTokenRequestInterceptor() {
        return template -> template.header(SERVICE_TOKEN_HEADER, serviceToken);
    }
}
