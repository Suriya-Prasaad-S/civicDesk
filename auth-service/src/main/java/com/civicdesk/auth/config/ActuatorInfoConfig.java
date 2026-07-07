package com.civicdesk.auth.config;

import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class ActuatorInfoConfig {

    @Bean
    public InfoContributor appInfoContributor() {
        return new InfoContributor() {
            @Override
            public void contribute(Builder builder) {
                builder.withDetail("app", Map.of(
                        "name", "auth-service",
                        "description", "CivicDesk Authentication and Authorization Service",
                        "version", "1.0.0"
                ));
            }
        };
    }
}
