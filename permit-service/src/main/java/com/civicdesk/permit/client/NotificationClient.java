package com.civicdesk.permit.client;

import com.civicdesk.permit.dto.NotificationRequest;
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

    public void sendNotification(NotificationRequest payload) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String jwtToken = getCurrentJwtToken();

        if (jwtToken != null) {
            headers.set("Authorization", jwtToken);
        }

        HttpEntity<NotificationRequest> requestEntity =
                new HttpEntity<>(payload, headers);

        String endpoint =
                notificationServiceUrl +
                        "/civicDesk/notification/createNotification";

        try {

            ResponseEntity<Void> response =
                    restTemplate.postForEntity(
                            endpoint,
                            requestEntity,
                            Void.class);

            log.info("Notification Payload: {}", payload);
            log.info("Notification URL: {}", endpoint);

            if (response.getStatusCode().is2xxSuccessful()) {

                log.info(
                        "Notification sent successfully for userId={}",
                        payload.getUserId());
            }

        } catch (Exception ex) {

            log.error(
                    "Notification Service Error: {}",
                    ex.getMessage(),
                    ex);
        }
    }

    private String getCurrentJwtToken() {

        ServletRequestAttributes attributes =
                (ServletRequestAttributes)
                        RequestContextHolder.getRequestAttributes();

        if (attributes != null) {

            return attributes
                    .getRequest()
                    .getHeader("Authorization");
        }

        return null;
    }
}