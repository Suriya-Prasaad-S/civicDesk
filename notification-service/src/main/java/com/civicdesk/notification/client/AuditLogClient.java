package com.civicdesk.notification.client;

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

    public void log(String userId, String action, String module) {
        if (userId == null || action == null || module == null) {
            log.warn("Cannot log audit event: missing required fields (userId={}, action={}, module={})", userId, action, module);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String jwtToken = getCurrentJwtToken();
        if (jwtToken != null) {
            headers.set("Authorization", jwtToken);
        }

        Map<String, String> payload = Map.of(
                "userId", userId,
                "action", action.toUpperCase(),
                "module", module.toUpperCase()
        );

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);
        String endpoint = gatewayUrl + "/civicDesk/audit/auditLogs";

        try {
            log.info("Sending audit log: action={} module={} to {}", action, module, endpoint);
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Audit log sent successfully.");
            } else {
                log.warn("Failed to send audit log. Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error calling audit-service: {}", e.getMessage());
        }
    }

    private String getCurrentJwtToken() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return attributes.getRequest().getHeader("Authorization");
        }
        return null;
    }
}
