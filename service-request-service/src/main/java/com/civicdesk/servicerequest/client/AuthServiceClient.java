package com.civicdesk.servicerequest.client;

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
public class AuthServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.auth-service.url:http://localhost:8081}")
    private String authServiceUrl;

    public Long getDepartmentSupervisorId(String departmentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String jwtToken = getCurrentJwtToken();
        if (jwtToken != null) {
            headers.set("Authorization", jwtToken);
        }

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        String endpoint = authServiceUrl + "/civicDesk/iam/departments/" + departmentId;

        try {
            log.info("Fetching supervisor for departmentId={} from {}", departmentId, endpoint);
            ResponseEntity<Map> response = restTemplate.exchange(endpoint, HttpMethod.GET, requestEntity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                Map data = (Map) body.get("data");
                if (data != null && data.get("departmentSupervisorId") != null) {
                    String supervisorIdStr = data.get("departmentSupervisorId").toString();
                    log.info("Supervisor ID found: {}", supervisorIdStr);
                    return Long.parseLong(supervisorIdStr);
                }
            }
            log.warn("No supervisor ID found for departmentId={}", departmentId);
        } catch (Exception e) {
            log.error("Error calling auth-service to get department: {}", e.getMessage(), e);
        }
        return null;
    }

    private String getCurrentJwtToken() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return attributes.getRequest().getHeader("Authorization");
        }
        return null;
    }
}
