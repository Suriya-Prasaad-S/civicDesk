package com.civicdesk.auth.dto;

import com.civicdesk.auth.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BroadcastNotificationRequest {

    @NotEmpty(message = "userIds must not be empty")
    private List<Long> userIds;

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "message is required")
    private String message;

    @NotNull(message = "notificationType is required")
    private NotificationType notificationType;
}
