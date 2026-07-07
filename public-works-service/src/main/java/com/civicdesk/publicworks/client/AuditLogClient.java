package com.civicdesk.publicworks.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogClient {

    private final RestTemplate restTemplate;

    @Value("${app.gateway.url:http://localhost:9090}")
    private String gatewayUrl;

    public void log(
            String userId,
            String action,
            String module) {

        if (userId == null ||
                action == null ||
                module == null) {

            log.warn(
                    "Cannot log audit event. Missing values");
            return;
        }

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON);

        String token =
                getCurrentJwtToken();

        if (token != null) {
            headers.set(
                    "Authorization",
                    token);
        }

        Map<String, String> payload =
                Map.of(
                        "userId", userId,
                        "action", action,
                        "module", module
                );

        HttpEntity<Map<String, String>> request =
                new HttpEntity<>(
                        payload,
                        headers);

        String endpoint =
                gatewayUrl +
                        "/civicDesk/audit/auditLogs";

        try {

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            endpoint,
                            request,
                            String.class);

            log.info(
                    "Audit Logged -> {}",
                    response.getStatusCode());

        } catch (Exception ex) {

            log.error(
                    "Audit Service Error",
                    ex);
        }
    }

    private String getCurrentJwtToken() {

        ServletRequestAttributes attributes =
                (ServletRequestAttributes)
                        RequestContextHolder
                                .getRequestAttributes();

        if (attributes != null) {

            return attributes
                    .getRequest()
                    .getHeader(
                            "Authorization");
        }

        return null;
    }
}