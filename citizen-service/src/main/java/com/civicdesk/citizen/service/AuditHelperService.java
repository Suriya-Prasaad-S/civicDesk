package com.civicdesk.citizen.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.civicdesk.citizen.client.AuthFeignClient;
import com.civicdesk.citizen.dto.request.CreateAuditLogRequest;
import com.civicdesk.citizen.util.SecurityContextUtil;

/**
 * Thin wrapper that writes audit-log entries to auth-service for this module ({@code CITIZEN}).
 *
 * <p>Audit logging is a best-effort side effect: failures are logged and swallowed so they never
 * roll back or break the business operation that triggered them.
 */
@Service
public class AuditHelperService {

    private static final Logger log = LoggerFactory.getLogger(AuditHelperService.class);

    private static final String MODULE = "CITIZEN";

    private final AuthFeignClient authFeignClient;

    public AuditHelperService(AuthFeignClient authFeignClient) {
        this.authFeignClient = authFeignClient;
    }

    /** Logs an action attributed to the current authenticated user (from the security context). */
    public void log(String action) {
        log(action, SecurityContextUtil.getCurrentUserId());
    }

    /**
     * Logs an action attributed to an explicit user id. Use this on flows with no security context
     * (e.g. public self-registration), passing the id of the citizen the action concerns.
     */
    public void log(String action, String userId) {
        try {
            authFeignClient.createAuditLog(
                    CreateAuditLogRequest.builder()
                            .userId(userId)
                            .action(action)
                            .module(MODULE)
                            .ipAddress(null)
                            .build());
        } catch (Exception e) {
            log.warn("Failed to write audit log (action={}, userId={}): {}",
                    action, userId, e.getMessage());
        }
    }
}
