package com.civicdesk.grievance.service;

import org.springframework.stereotype.Service;

import com.civicdesk.grievance.client.NotificationClient;
import com.civicdesk.grievance.dto.request.SendNotificationRequest;
import com.civicdesk.grievance.enums.NotificationType;
import com.civicdesk.grievance.enums.ReferenceType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationHelperService {

    private final NotificationClient notificationClient;

    public void notify(
            Long userId,
            String title,
            String message,
            NotificationType type,
            Long referenceId,
            ReferenceType referenceType) {

        SendNotificationRequest request =
                new SendNotificationRequest();

        request.setUserId(userId);
        request.setTitle(title);
        request.setMessage(message);
        request.setNotificationType(type);
        request.setReferenceId(referenceId);
        request.setReferenceType(referenceType);

        notificationClient.sendNotification(request);
    }
}