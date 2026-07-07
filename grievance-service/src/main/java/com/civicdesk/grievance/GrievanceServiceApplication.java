package com.civicdesk.grievance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GrievanceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GrievanceServiceApplication.class, args);
    }
}
