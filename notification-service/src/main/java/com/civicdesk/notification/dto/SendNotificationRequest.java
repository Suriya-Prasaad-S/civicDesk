package com.civicdesk.notification.dto;

import com.civicdesk.notification.enums.NotificationType;
import com.civicdesk.notification.enums.ReferenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendNotificationRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "message is required")
    private String message;

    @NotNull(message = "notificationType is required")
    private NotificationType notificationType;

    private Long referenceId;

    private ReferenceType referenceType;
}
