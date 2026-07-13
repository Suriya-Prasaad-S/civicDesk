package com.civicdesk.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Monitoring & Metrics Configuration.
 * Provides Prometheus metrics, health checks, and custom metrics.
 */
@Configuration
public class MonitoringConfiguration {
    private static final Logger log = LoggerFactory.getLogger(MonitoringConfiguration.class);

    /**
     * Custom health indicator for gateway.
     */
    @Bean
    public GatewayHealthIndicator gatewayHealthIndicator() {
        log.info("Gateway health indicator initialized");
        return new GatewayHealthIndicator();
    }
}
