package com.civicdesk.publicworks.dto;

import lombok.Data;

@Data
public class NotificationRequest {

    private Long userId;

    private String title;

    private String message;

    private String notificationType;

    private Long referenceId;

    private String referenceType;
}