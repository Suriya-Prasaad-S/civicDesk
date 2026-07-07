package com.civicdesk.auth.controller;

import com.civicdesk.auth.dto.response.AuditLogResponse;
import com.civicdesk.auth.response.ApiResponse;
import com.civicdesk.auth.response.PageResponse;
import com.civicdesk.auth.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import com.civicdesk.auth.dto.request.CreateAuditLogRequest;

@RestController
@RequestMapping("/audit/auditLogs")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADM', 'CO')")
    public ResponseEntity<ApiResponse> getAuditLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<AuditLogResponse> logs = auditService.getAll(userId, action, module, page, size);
        return ResponseEntity.ok(ApiResponse.data(logs));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADM', 'CO')")
    public ResponseEntity<ApiResponse> getAuditLogById(@PathVariable String id) {
        AuditLogResponse log = auditService.getById(id);
        return ResponseEntity.ok(ApiResponse.data(log));
    }

    @PostMapping
    // allow only authenticated service accounts / admin roles to post audit entries
    @PreAuthorize("hasAnyRole('ADM', 'CO')")
    public ResponseEntity<ApiResponse> createAuditLog(HttpServletRequest request,
                                                      @RequestBody CreateAuditLogRequest req) {
        if (req.getUserId() == null || req.getUserId().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("userId is required"));
        }
        if (req.getAction() == null || req.getAction().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("action is required"));
        }
        if (req.getModule() == null || req.getModule().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("module is required"));
        }

        // Normalize values
        String userId = req.getUserId().trim();
        String action = req.getAction().trim().toUpperCase();
        String module = req.getModule().trim().toUpperCase();
        String ip = resolveClientIp(request, req.getIpAddress());

        auditService.log(userId, action, module, ip);
        return ResponseEntity.ok(ApiResponse.of("Audit Log Added", null));
    }

    private String resolveClientIp(HttpServletRequest request, String fallbackIp) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            int commaIndex = ip.indexOf(',');
            if (commaIndex > -1) {
                ip = ip.substring(0, commaIndex).trim();
            }
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        if (ip == null || ip.isBlank()) {
            ip = fallbackIp;
        }
        if (ip == null || ip.isBlank()) {
            ip = "UNKNOWN";
        }
        return ip;
    }
}
