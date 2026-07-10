package com.civicdesk.analytics.client;

import com.civicdesk.analytics.dto.request.CreateAuditLogRequest;
import com.civicdesk.analytics.dto.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditClient {

    private final RestTemplate restTemplate;

    @Value("${app.auth-service.url}")
    private String authServiceUrl;

    /**
     * Send audit log to auth-service
     */
    public void logAudit(String userId, String action, String module, String ip) {
        String jwtToken = getCurrentJwtToken();
        
        try {
            CreateAuditLogRequest auditRequest = CreateAuditLogRequest.builder()
                    .userId(userId)
                    .action(action)
                    .module(module)
                    .ipAddress(ip)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (jwtToken != null) {
                headers.set("Authorization", jwtToken);
            }

            HttpEntity<CreateAuditLogRequest> requestEntity = new HttpEntity<>(auditRequest, headers);
            String endpoint = authServiceUrl + "/civicDesk/audit/auditLogs";

            log.info("Sending audit log for userId={}, action={}, module={} to {}", userId, action, module, endpoint);
            ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
                    endpoint,
                    requestEntity,
                    ApiResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Audit log sent successfully for userId={}", userId);
            } else {
                log.warn("Failed to send audit log. Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error sending audit log to auth-service: {}", e.getMessage(), e);
            // Don't throw exception - audit logging should not block the request
        }
    }

    private String getCurrentJwtToken() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest().getHeader("Authorization");
            }
        } catch (Exception e) {
            log.warn("Failed to extract JWT token from current request", e);
        }
        return null;
    }
}
