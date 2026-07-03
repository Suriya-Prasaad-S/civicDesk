package com.civicdesk.servicerequest.config;

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
                        .title("CivicDesk - Service Request Service API")
                        .description("""
                                Service Request Management for CivicDesk.

                                **Workflow:** SUBMITTED → UNDER_REVIEW → PENDING_DOCUMENTS → APPROVED → COMPLETED

                                **Who can do what:**
                                - `ROLE_CITIZEN` — Browse catalog, submit requests, upload documents
                                - `ROLE_FIELD_OFFICER` — View assigned requests, update status
                                - `ROLE_DEPT_SUPERVISOR` — Full queue management, assign officers, approve/reject
                                - `ROLE_COMPLIANCE` — Read-only access to all requests and reports
                                - `ROLE_ADMIN` — Full access including catalog management

                                **Service Catalog:** Public read — no token needed to browse services.
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
