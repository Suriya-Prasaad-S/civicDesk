package com.civicdesk.gateway;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * CivicDesk API Gateway Application.
 * 
 * Implements microservice patterns:
 * - Authentication & Authorization (JWT)
 * - Request Routing to 8 microservices
 * - Rate Limiting (Redis-backed, distributed)
 * - Enhanced Logging with Trace ID Correlation
 * - Role-Based Access Control (RBAC)
 * - Health Checks & Monitoring (Prometheus)
 * - Service Resilience with Timeout & Retry Policies
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
