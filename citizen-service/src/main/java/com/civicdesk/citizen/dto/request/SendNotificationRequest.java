package com.civicdesk.citizen.dto.request;

import com.civicdesk.citizen.enums.NotificationType;
import com.civicdesk.citizen.enums.ReferenceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for notification-service's send endpoint ({@code POST /notification/send}).
 * {@code userId} and {@code referenceId} are Long (notification-service uses numeric ids);
 * citizen ids are numeric strings, so callers convert via {@code Long.valueOf(...)}.
 */
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
