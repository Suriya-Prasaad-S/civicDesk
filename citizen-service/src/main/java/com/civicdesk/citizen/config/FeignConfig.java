package com.civicdesk.citizen.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign configuration for internal service-to-service calls.
 *
 * <p>Auth Service's user-lookup endpoints (e.g. {@code /iam/users/by-email}) require an
 * authenticated caller, but the public citizen-registration flow has no end-user JWT to forward.
 * This interceptor attaches the shared internal key ({@code X-Internal-Key}) to every Feign
 * request, which Auth Service trusts as an internal service call.
 */
@Configuration
public class FeignConfig {

    private static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

    @Value("${app.internal.api-key}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor internalKeyRequestInterceptor() {
        return template -> template.header(INTERNAL_KEY_HEADER, internalApiKey);
    }
}
