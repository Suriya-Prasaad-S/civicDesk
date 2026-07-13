package com.civicdesk.grievance.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Feign client configuration that forwards the JWT token from the incoming request
 * to any outgoing Feign calls to other services.
 */
@Configuration
public class FeignClientConfig implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {

        System.out.println("Feign interceptor executed");

        ServletRequestAttributes attributes =
                (ServletRequestAttributes)
                        RequestContextHolder.getRequestAttributes();

        if (attributes != null) {

            HttpServletRequest request =
                    attributes.getRequest();

            String authHeader =
                    request.getHeader("Authorization");

            System.out.println("Auth Header = " + authHeader);

            if (authHeader != null &&
                    !authHeader.isEmpty()) {

                template.header("Authorization", authHeader);
            }
        }
    }
}
