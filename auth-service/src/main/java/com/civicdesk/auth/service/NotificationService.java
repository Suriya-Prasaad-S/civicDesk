package com.civicdesk.auth.service;

import com.civicdesk.auth.dto.*;
import com.civicdesk.auth.enums.NotificationType;
import com.civicdesk.auth.enums.ReferenceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final RestTemplate restTemplate;

    @Value("${app.notification-service.url:http://localhost:8087}")
    private String notificationServiceUrl;

    // ─── SEND ─────────────────────────────────────────────────────────────────

    /**
     * Send notification to a specific user via notification-service
     * Does NOT throw exceptions - notification failures should not block main operations
     */
    public NotificationResponse send(SendNotificationRequest request) {
        try {
            HttpEntity<SendNotificationRequest> entity = createHttpEntity(request);
            String endpoint = notificationServiceUrl + "/civicDesk/notification/send";

            log.info("Calling notification-service: POST {}", endpoint);
            NotificationResponse response = restTemplate.postForObject(
                    endpoint,
                    entity,
                    NotificationResponse.class
            );
            log.info("Notification sent successfully: id={} userId={}", 
                    response.getNotificationId(), response.getUserId());
            return response;
        } catch (Exception e) {
            log.warn("Failed to send notification via notification-service - continuing anyway: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Broadcast notification to multiple users via notification-service
     * Does NOT throw exceptions - notification failures should not block main operations
     */
    public List<NotificationResponse> broadcast(BroadcastNotificationRequest request) {
        try {
            HttpEntity<BroadcastNotificationRequest> entity = createHttpEntity(request);
            String endpoint = notificationServiceUrl + "/civicDesk/notification/broadcast";

            log.info("Calling notification-service: POST {} for {} users", endpoint, request.getUserIds().size());
            NotificationResponse[] responses = restTemplate.postForObject(
                    endpoint,
                    entity,
                    NotificationResponse[].class
            );
            log.info("Broadcast sent to {} users successfully", responses != null ? responses.length : 0);
            return List.of(responses != null ? responses : new NotificationResponse[0]);
        } catch (Exception e) {
            log.warn("Failed to broadcast notification via notification-service - continuing anyway: {}", e.getMessage());
            return List.of();
        }
    }

    // ─── INTERNAL ALERT METHODS ───────────────────────────────────────────────

    /**
     * Send account created alert - called when new user is created
     */
    public void sendAccountCreatedAlert(Long userId, String userEmail) {
        try {
            send(SendNotificationRequest.builder()
                    .userId(userId)
                    .title("Welcome to CivicDesk")
                    .message("Your account has been successfully created. Email: " + userEmail)
                    .notificationType(NotificationType.ACCOUNT_CREATED)
                    .referenceId(userId)
                    .referenceType(ReferenceType.USER)
                    .build());
            log.info("Account created alert sent to userId={}", userId);
        } catch (Exception e) {
            log.error("Failed to send account created alert to userId={}: {}", userId, e.getMessage());
        }
    }

    /**
     * Send password changed alert - called when user changes password
     */
    public void sendPasswordChangedAlert(Long userId) {
        try {
            send(SendNotificationRequest.builder()
                    .userId(userId)
                    .title("Password Changed")
                    .message("Your account password has been recently changed. If this wasn't you, please contact support immediately.")
                    .notificationType(NotificationType.PASSWORD_CHANGED)
                    .referenceId(userId)
                    .referenceType(ReferenceType.USER)
                    .build());
            log.info("Password changed alert sent to userId={}", userId);
        } catch (Exception e) {
            log.error("Failed to send password changed alert to userId={}: {}", userId, e.getMessage());
        }
    }

    /**
     * Send account suspended alert - called when admin suspends account
     */
    public void sendAccountSuspendedAlert(Long userId) {
        try {
            send(SendNotificationRequest.builder()
                    .userId(userId)
                    .title("Account Suspended")
                    .message("Your account has been suspended. Please contact your administrator for more information.")
                    .notificationType(NotificationType.ACCOUNT_SUSPENDED)
                    .referenceId(userId)
                    .referenceType(ReferenceType.USER)
                    .build());
            log.info("Account suspended alert sent to userId={}", userId);
        } catch (Exception e) {
            log.error("Failed to send account suspended alert to userId={}: {}", userId, e.getMessage());
        }
    }

    /**
     * Send account reactivated alert - called when admin reactivates account
     */
    public void sendAccountReactivatedAlert(Long userId) {
        try {
            send(SendNotificationRequest.builder()
                    .userId(userId)
                    .title("Account Reactivated")
                    .message("Your account has been successfully reactivated. You can now log in as normal.")
                    .notificationType(NotificationType.ACCOUNT_REACTIVATED)
                    .referenceId(userId)
                    .referenceType(ReferenceType.USER)
                    .build());
            log.info("Account reactivated alert sent to userId={}", userId);
        } catch (Exception e) {
            log.error("Failed to send account reactivated alert to userId={}: {}", userId, e.getMessage());
        }
    }

    /**
     * Send login alert - called when user successfully logs in
     */
    public void sendLoginAlert(Long userId, String device) {
        try {
            send(SendNotificationRequest.builder()
                    .userId(userId)
                    .title("New Login")
                    .message("A new login to your account was detected from " + device + ". If this wasn't you, please change your password.")
                    .notificationType(NotificationType.LOGIN_ALERT)
                    .referenceId(userId)
                    .referenceType(ReferenceType.SECURITY)
                    .build());
            log.info("Login alert sent to userId={} from device={}", userId, device);
        } catch (Exception e) {
            log.error("Failed to send login alert to userId={}: {}", userId, e.getMessage());
        }
    }

    // ─── HELPER METHODS ───────────────────────────────────────────────────────

    /**
     * Create HTTP entity with Authorization header
     */
    private <T> HttpEntity<T> createHttpEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Propagate JWT token from incoming request to notification-service
        String jwtToken = getCurrentJwtToken();
        if (jwtToken != null && !jwtToken.isBlank()) {
            headers.set("Authorization", jwtToken);
            log.debug("JWT token propagated to notification-service");
        }
        
        return new HttpEntity<>(body, headers);
    }

    /**
     * Get JWT token from current request context
     */
    private String getCurrentJwtToken() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest().getHeader("Authorization");
            }
        } catch (Exception e) {
            log.debug("Could not retrieve JWT token from request context: {}", e.getMessage());
        }
        return null;
    }
}
