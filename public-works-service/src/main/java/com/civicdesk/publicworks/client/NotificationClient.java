package com.civicdesk.publicworks.client;

import com.civicdesk.publicworks.dto.NotificationRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class NotificationClient {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationClient.class);

    private final RestTemplate restTemplate;

    @Value("${app.notification-service.url}")
    private String notificationServiceUrl;

    public NotificationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendNotification(
            NotificationRequest payload) {

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

        HttpEntity<NotificationRequest> entity =
                new HttpEntity<>(
                        payload,
                        headers);

        String endpoint =
                notificationServiceUrl +
                        "/civicDesk/notification/createNotification";

        try {

            log.info(
                    """
                    Notification Payload
                    userId={}
                    title={}
                    message={}
                    notificationType={}
                    referenceId={}
                    referenceType={}
                    """,
                    payload.getUserId(),
                    payload.getTitle(),
                    payload.getMessage(),
                    payload.getNotificationType(),
                    payload.getReferenceId(),
                    payload.getReferenceType());
            ResponseEntity<Void> response =
                    restTemplate.postForEntity(
                            endpoint,
                            entity,
                            Void.class);

            log.info(
                    "Notification sent. Status={}",
                    response.getStatusCode());

        } catch (Exception ex) {

            log.error(
                    "Notification service error",
                    ex);
        }
    }

    private String getCurrentJwtToken() {

        ServletRequestAttributes attributes =
                (ServletRequestAttributes)
                        RequestContextHolder
                                .getRequestAttributes();

        if (attributes == null) {
            return null;
        }

        HttpServletRequest request =
                attributes.getRequest();

        return request.getHeader(
                "Authorization");
    }
}