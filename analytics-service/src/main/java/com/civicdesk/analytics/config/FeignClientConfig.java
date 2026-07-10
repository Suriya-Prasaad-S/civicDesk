package com.civicdesk.analytics.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Propagates Authorization header from the incoming request
 * to outgoing Feign requests.
 */
@Configuration
public class FeignClientConfig implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {

        ServletRequestAttributes attributes =
                (ServletRequestAttributes)
                        RequestContextHolder.getRequestAttributes();

        if (attributes != null) {

            HttpServletRequest request =
                    attributes.getRequest();

            String authHeader =
                    request.getHeader("Authorization");

            if (authHeader != null && !authHeader.isBlank()) {
                template.header("Authorization", authHeader);
            }
        }
    }
}