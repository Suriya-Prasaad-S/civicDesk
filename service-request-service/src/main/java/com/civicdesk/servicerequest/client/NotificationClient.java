package com.civicdesk.servicerequest.client;

import com.civicdesk.servicerequest.dto.request.NotificationRequest;
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
public class NotificationClient {

    private final RestTemplate restTemplate;

    @Value("${app.notification-service.url}")
    private String notificationServiceUrl;

    @Value("${app.service.token:}")
    private String serviceToken;

    public void sendNotification(NotificationRequest payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String jwtToken = getCurrentJwtToken();
        if (jwtToken != null) {
            headers.set("Authorization", jwtToken);
        }
        if (serviceToken != null && !serviceToken.isBlank()) {
            headers.set("X-Service-Token", serviceToken);
        }

        HttpEntity<NotificationRequest> requestEntity = new HttpEntity<>(payload, headers);
        String endpoint = notificationServiceUrl + "/civicDesk/notification/createNotification";

        try {
            log.info("Sending notification for userId={} to {}", payload.getUserId(), endpoint);
            ResponseEntity<Void> response = restTemplate.postForEntity(endpoint, requestEntity, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Notification sent successfully.");
            } else {
                log.warn("Failed to deliver notification. Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error calling notification-service: {}", e.getMessage(), e);
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
