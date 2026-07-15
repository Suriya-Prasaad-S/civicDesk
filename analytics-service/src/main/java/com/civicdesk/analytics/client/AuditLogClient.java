package com.civicdesk.analytics.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.civicdesk.analytics.config.FeignClientConfig;
import com.civicdesk.analytics.dto.request.CreateAuditLogRequest;

@FeignClient(
        name = "auth-service",
        path = "/civicDesk",
        configuration = FeignClientConfig.class
)
public interface AuditLogClient {

    @PostMapping("/audit/auditLogs")
    void createAuditLog(
            @RequestBody CreateAuditLogRequest request
    );
}