package com.civicdesk.citizen.config;

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
                        .title("CivicDesk - Citizen Service API")
                        .description("""
                                Citizen Profile & Registration Service for CivicDesk.

                                **Who can do what:**
                                - `ROLE_CITIZEN` — Create/view/update own profile; manage own document wallet
                                - `ROLE_FIELD_OFFICER` — View citizen profiles and documents
                                - `ROLE_DEPT_SUPERVISOR` — View citizens; verify/reject documents
                                - `ROLE_COMPLIANCE` — Read-only access to citizen profiles
                                - `ROLE_ADMIN` — Full access including status management

                                **Usage:** Login via Auth Service (port 8081), then enter: `Bearer <token>`
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("CivicDesk Team").email("admin@civicdesk.gov")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
