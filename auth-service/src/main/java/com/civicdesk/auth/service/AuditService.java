package com.civicdesk.auth.service;

import com.civicdesk.auth.dto.response.AuditLogResponse;
import com.civicdesk.auth.response.PageResponse;

public interface AuditService {
    void log(String userId, String action, String module, String ip);
    PageResponse<AuditLogResponse> getAll(String userId, String action, String module, int page, int size);
    AuditLogResponse getById(String id);
}
