package com.civicdesk.grievance.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.civicdesk.grievance.config.FeignClientConfig;
import com.civicdesk.grievance.dto.request.CreateAuditLogRequest;
import com.civicdesk.grievance.dto.response.ApiResponse;

@FeignClient(
        name = "auth-service",
        // url = "${auth.service.url}",
        path = "/civicDesk",
        configuration = FeignClientConfig.class
)
public interface AuthClient {

    @GetMapping("/iam/users/{id}")
    ApiResponse getUserById(@PathVariable("id") String id);

    @GetMapping("/iam/users/{id}/roles")
    ApiResponse getUserRoles(@PathVariable("id") String id);

    @GetMapping("/iam/departments/{id}")
    ApiResponse getDepartmentById(@PathVariable("id") String id);

    @PostMapping("/audit/auditLogs")
    void createAuditLog(
            @RequestBody CreateAuditLogRequest request
    );
}
