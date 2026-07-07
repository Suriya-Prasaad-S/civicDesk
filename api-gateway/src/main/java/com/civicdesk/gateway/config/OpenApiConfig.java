package com.civicdesk.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for API Gateway.
 * Configures Swagger UI aggregation for all downstream microservices.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CivicDesk API Gateway")
                        .description("Central API Gateway aggregating Swagger documentation from all CivicDesk microservices")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CivicDesk Team")
                                .email("support@civicdesk.gov"))
                        .license(new License()
                                .name("Apache License 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
