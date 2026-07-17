package com.civicdesk.citizen.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for auth-service's audit endpoint ({@code POST /audit/auditLogs}). {@code userId} is a
 * String here (auth-service trims it and uppercases {@code action}/{@code module} on persist).
 */
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
