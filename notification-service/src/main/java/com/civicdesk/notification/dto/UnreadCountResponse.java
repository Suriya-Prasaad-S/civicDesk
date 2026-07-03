package com.civicdesk.notification.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class UnreadCountResponse {
    private Long userId;
    private long unreadCount;
}
