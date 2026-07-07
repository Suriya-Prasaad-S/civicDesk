package com.civicdesk.servicerequest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationRequest {
    private Long userId;
    private String title;
    private String message;
    private String notificationType;
    private Long referenceId;
    private String referenceType;
}
