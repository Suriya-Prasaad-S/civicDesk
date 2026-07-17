package com.civicdesk.citizen.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.civicdesk.citizen.client.NotificationFeignClient;
import com.civicdesk.citizen.dto.request.SendNotificationRequest;
import com.civicdesk.citizen.enums.NotificationType;
import com.civicdesk.citizen.enums.ReferenceType;

/**
 * Thin wrapper that sends notifications through notification-service.
 *
 * <p>Citizen ids are numeric strings; notification-service expects Long {@code userId}/
 * {@code referenceId}, so they are converted here. Sending is a best-effort side effect: any
 * failure (including a non-numeric id) is logged and swallowed so it never breaks the caller.
 */
@Service
public class NotificationHelperService {

    private static final Logger log = LoggerFactory.getLogger(NotificationHelperService.class);

    private final NotificationFeignClient notificationClient;

    public NotificationHelperService(NotificationFeignClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    public void notify(
            String userId,
            String title,
            String message,
            NotificationType type,
            String referenceId,
            ReferenceType referenceType) {
        try {
            SendNotificationRequest request = SendNotificationRequest.builder()
                    .userId(Long.valueOf(userId))
                    .title(title)
                    .message(message)
                    .notificationType(type)
                    .referenceId(referenceId == null ? null : Long.valueOf(referenceId))
                    .referenceType(referenceType)
                    .build();
            notificationClient.sendNotification(request);
        } catch (Exception e) {
            log.warn("Failed to send notification (type={}, userId={}): {}",
                    type, userId, e.getMessage());
        }
    }
}
