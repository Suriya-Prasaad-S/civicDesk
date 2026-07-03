package com.civicdesk.servicerequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class ServiceRequestApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceRequestApplication.class, args);
        log.info("===========================================");
        log.info("  CivicDesk Service Request Service — STARTED");
        log.info("  Swagger UI: http://localhost:8083/swagger-ui.html");
        log.info("===========================================");
    }
}
