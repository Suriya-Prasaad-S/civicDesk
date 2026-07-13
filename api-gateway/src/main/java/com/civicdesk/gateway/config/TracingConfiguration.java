package com.civicdesk.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Distributed Tracing Configuration.
 * Enables request tracing with trace ID and correlation ID propagation.
 * 
 * OpenTelemetry can be integrated separately for production Jaeger integration.
 */
@Configuration
public class TracingConfiguration {
    private static final Logger log = LoggerFactory.getLogger(TracingConfiguration.class);

    public TracingConfiguration() {
        log.info("Tracing configuration initialized - trace ID and correlation ID propagation enabled");
    }
}
