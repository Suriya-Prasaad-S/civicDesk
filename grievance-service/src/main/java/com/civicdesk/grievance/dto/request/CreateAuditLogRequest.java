package com.civicdesk.grievance.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateAuditLogRequest {

    private String userId;
    private String action;
    private String module;
    private String ipAddress;
}