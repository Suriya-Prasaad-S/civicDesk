package com.civicdesk.citizen;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@Slf4j
public class CitizenServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CitizenServiceApplication.class, args);
        log.info("===========================================");
        log.info("  CivicDesk Citizen Service — STARTED");
        log.info("  Swagger UI: http://localhost:8082/swagger-ui.html");
        log.info("===========================================");
    }
}
