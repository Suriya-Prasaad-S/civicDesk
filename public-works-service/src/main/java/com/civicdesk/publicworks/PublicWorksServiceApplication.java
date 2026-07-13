package com.civicdesk.publicworks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class PublicWorksServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PublicWorksServiceApplication.class, args);
    }
}
