package com.civicdesk.auth.dto;

import com.civicdesk.auth.enums.NotificationType;
import com.civicdesk.auth.enums.ReferenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
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
