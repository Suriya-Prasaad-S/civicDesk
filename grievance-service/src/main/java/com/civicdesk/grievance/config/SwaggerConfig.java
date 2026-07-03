package com.civicdesk.grievance.config;

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
                        .title("CivicDesk — Grievance Service API")
                        .version("1.0")
                        .description("""
                                Grievance Management with L1/L2/L3 Escalation and SLA Tracking.

                                **Roles:**
                                - `ROLE_CITIZEN` — Submit grievance, track own grievances, add comments
                                - `ROLE_FIELD_OFFICER` — View assigned grievances, update status to IN_PROGRESS/RESOLVED
                                - `ROLE_DEPT_SUPERVISOR` — Assign grievances to officers, escalate, view department grievances
                                - `ROLE_PWE` — View and act on L3 escalated grievances
                                - `ROLE_COMPLIANCE` — Read-only access to all grievances and SLA breach reports
                                - `ROLE_ADMIN` — Full access: assign, escalate, update, close, override

                                **SLA Deadlines:**
                                - CRITICAL: 2 days | HIGH: 5 days | MEDIUM: 10 days | LOW: 15 days

                                **Escalation Flow:**
                                - L1 (Field Officer) → L2 (Dept Supervisor) → L3 (Admin/PWE)
                                - On SLA breach, grievance is escalated to the next level automatically
                                """))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your JWT token here")));
    }
}
