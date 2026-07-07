package com.civicdesk.permit.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CivicDesk - Permit & License Service API")
                        .description("""
                                Permit and License Management for CivicDesk.

                                **Permit Types:** BUILDING_PERMIT, TRADE_LICENSE, EVENT_PERMISSION, ADVERTISEMENT_LICENSE

                                **Permit Workflow:**
                                APPLIED → UNDER_REVIEW → INSPECTION_SCHEDULED → APPROVED / REJECTED

                                **Inspection Outcomes:** PASS → auto-approves permit | FAIL → auto-rejects | CONDITIONAL_APPROVAL → supervisor decides

                                **Who can do what:**
                                - `ROLE_CITIZEN` — Apply for permits, track status, renew approved permits
                                - `ROLE_FIELD_OFFICER` — View assigned inspections, conduct inspections with geo-coordinates
                                - `ROLE_DEPT_SUPERVISOR` — Schedule inspections, approve/reject permits, cancel inspections
                                - `ROLE_COMPLIANCE` — Read-only access to all permits and inspections
                                - `ROLE_ADMIN` — Full access
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("CivicDesk Team").email("admin@civicdesk.gov")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
