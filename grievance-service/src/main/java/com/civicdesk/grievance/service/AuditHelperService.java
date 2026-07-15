package com.civicdesk.grievance.service;

import org.springframework.stereotype.Service;

import com.civicdesk.grievance.client.AuthClient;
import com.civicdesk.grievance.dto.request.CreateAuditLogRequest;
import com.civicdesk.grievance.security.JwtUserContext;

@Service
public class AuditHelperService {

    private final AuthClient auditLogClient;

    public AuditHelperService(AuthClient auditLogClient) {
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