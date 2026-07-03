package com.civicdesk.notification.dto;

import com.civicdesk.notification.enums.NotificationType;
import com.civicdesk.notification.enums.ReferenceType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class NotificationResponse {
    private Long notificationId;
    private Long userId;
    private String title;
    private String message;
    private NotificationType notificationType;
    private Long referenceId;
    private ReferenceType referenceType;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
