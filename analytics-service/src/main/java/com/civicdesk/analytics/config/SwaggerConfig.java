package com.civicdesk.analytics.config;

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
                        .title("CivicDesk — Analytics Service API")
                        .version("1.0")
                        .description("""
                                Civic Reports and Dashboard Metrics for CivicDesk administrators.

                                **Roles:**
                                - `ROLE_COMPLIANCE` — View reports, access dashboard metrics and SLA compliance data
                                - `ROLE_DEPT_SUPERVISOR` — View reports and metrics for their department
                                - `ROLE_ADMIN` — Full access: generate reports, view all metrics, delete reports

                                **Dashboard Metrics:**
                                The `/api/analytics/dashboard` endpoint returns a real-time snapshot of counts
                                across grievances, permits, service requests, and work orders — stored in this
                                service's own DB as snapshot records that are updated on report generation.

                                **Report Storage:**
                                Generated reports are stored as JSON snapshots in `civic_reports` table.
                                Each report includes the parameters used and the result data at generation time.
                                """))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
