package com.civicdesk.analytics.service;

import org.springframework.stereotype.Service;

import com.civicdesk.analytics.client.AuditLogClient;
import com.civicdesk.analytics.dto.request.CreateAuditLogRequest;
import com.civicdesk.analytics.security.JwtUserContext;


@Service
public class AuditHelperService {

    private final AuditLogClient auditLogClient;

    public AuditHelperService(AuditLogClient auditLogClient) {
        this.auditLogClient = auditLogClient;
    }

    public void log(String action) {

        auditLogClient.createAuditLog(
                CreateAuditLogRequest.builder()
                        .userId(JwtUserContext.getCurrentUserId())
                        .action(action)
                        .module("GRIEVANCE")
                        .ipAddress(null)
                        .build()
        );
    }
}