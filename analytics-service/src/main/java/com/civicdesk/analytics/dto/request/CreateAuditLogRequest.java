package com.civicdesk.analytics.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAuditLogRequest {
    private String userId;
    private String action;
    private String module;
    private String ipAddress;
}
