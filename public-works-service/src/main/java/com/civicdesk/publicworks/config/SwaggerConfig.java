package com.civicdesk.publicworks.config;

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
                        .title("CivicDesk — Public Works Service API")
                        .version("1.0")
                        .description("""
                                Public Works Engine — Work Orders, Milestones, Contractor Assignment, Budget Tracking.

                                **Roles:**
                                - `PUBLIC` — Read-only view of all work orders and their statuses (no token required)
                                - `ROLE_CITIZEN` — Authenticated read; can view work orders and milestone progress
                                - `ROLE_FIELD_OFFICER` / `ROLE_PWE` — Update milestone progress on assigned work orders
                                - `ROLE_DEPT_SUPERVISOR` — Create work orders, assign contractors, manage milestones, update status
                                - `ROLE_COMPLIANCE` — Read-only: budget reports, overrun alerts
                                - `ROLE_ADMIN` — Full access including budget edits and cancellations

                                **Budget Tracking:**
                                - budgetAllocated set at creation; budgetSpent updated by field officer/PWE
                                - Budget overrun report available to COMPLIANCE and ADMIN
                                """))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your JWT token here")))


                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement()
                        .addList("BearerAuth"));
    }
}
