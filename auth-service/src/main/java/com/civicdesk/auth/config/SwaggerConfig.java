package com.civicdesk.auth.config;

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

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI civicDeskOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CivicDesk - Auth Service API")
                        .description("""
                                Identity & Access Management for CivicDesk Government Citizen Services System.

                                **Roles:**
                                - `ROLE_CITIZEN` — Self-registered citizens
                                - `ROLE_FIELD_OFFICER` — Field inspection officers
                                - `ROLE_DEPT_SUPERVISOR` — Department supervisors
                                - `ROLE_PWE` — Public Works Engineers
                                - `ROLE_COMPLIANCE` — Compliance officers
                                - `ROLE_ADMIN` — Government administrators

                                **Usage:** Login to get a JWT token, then click 'Authorize' and enter: `Bearer <token>`
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CivicDesk Team")
                                .email("admin@civicdesk.gov")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from /api/auth/login")));
    }
}
