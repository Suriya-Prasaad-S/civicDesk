package com.civicdesk.grievance.dto.request;

import com.civicdesk.grievance.enums.NotificationType;
import com.civicdesk.grievance.enums.ReferenceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendNotificationRequest {

    private Long userId;

    private String title;

    private String message;

    private NotificationType notificationType;

    private Long referenceId;

    private ReferenceType referenceType;
}