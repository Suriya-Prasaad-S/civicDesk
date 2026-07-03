package com.civicdesk.notification.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CivicDesk — Notification Service API")
                        .version("1.0")
                        .description("""
                                In-app Notification Management for all CivicDesk users.

                                **Roles:**
                                - `ROLE_CITIZEN` — Read own notifications, mark as read, get unread count
                                - `ROLE_FIELD_OFFICER` / `ROLE_DEPT_SUPERVISOR` / `ROLE_PWE` / `ROLE_COMPLIANCE` — Same as citizen (own notifications)
                                - `ROLE_ADMIN` — Send notifications to any user, broadcast to multiple users, view all notifications

                                **Design Note:**
                                In production, other microservices post to this service's internal endpoint to push
                                notifications when status changes occur (e.g., grievance escalated, permit approved).
                                For this phase, the ADMIN can trigger them manually via the API.
                                """))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
