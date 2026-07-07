package com.civicdesk.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CivicDesk — API Gateway")
                        .version("1.0")
                        .description("""
                                Single entry point for all CivicDesk microservices.

                                **Port:** 8080

                                **Services routed:**
                                | Service | Port | Base Path |
                                |---------|------|-----------|
                                | auth-service | 8081 | /api/auth/** |
                                | citizen-service | 8082 | /api/citizens/**, /api/citizen-documents/** |
                                | service-request-service | 8083 | /api/service-catalog/**, /api/service-requests/** |
                                | permit-service | 8084 | /api/permits/**, /api/inspections/** |
                                | grievance-service | 8085 | /api/grievances/** |
                                | public-works-service | 8086 | /api/work-orders/**, /api/milestones/** |
                                | notification-service | 8087 | /api/notifications/** |
                                | analytics-service | 8088 | /api/analytics/** |

                                **JWT Pre-validation:**
                                All requests (except public paths) are validated at the gateway before forwarding.
                                Invalid or missing tokens receive 401 immediately — downstream services are never hit.

                                **Public paths (no token required):**
                                - POST /api/auth/register
                                - POST /api/auth/login
                                - GET /api/service-catalog/**
                                - GET /api/work-orders/**
                                - Swagger/OpenAPI docs

                                **How to use:** Click "Authorize" and paste your JWT from /api/auth/login.
                                Select a service from the top-right dropdown to switch Swagger docs.
                                """))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token from POST /api/auth/login")));
    }
}
