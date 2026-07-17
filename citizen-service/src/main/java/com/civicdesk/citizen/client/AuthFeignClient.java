package com.civicdesk.citizen.client;

import com.civicdesk.citizen.config.FeignConfig;
import com.civicdesk.citizen.dto.request.CreateAuditLogRequest;
import com.civicdesk.citizen.dto.request.RegisterRequest;
import com.civicdesk.citizen.dto.response.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "auth-service",
        // url = "${auth-service.url:http://localhost:8081}",
        path = "/civicDesk",
        configuration = FeignConfig.class
)
public interface AuthFeignClient {

    // String AUTH_SERVICE_BASE_URL = "http://localhost:8081/civicDesk";

    @PostMapping("/iam/auth/register")
    void register(@RequestBody RegisterRequest request,
                  @RequestHeader(value = "X-Forwarded-For", required = false) String ip);

    @GetMapping("/iam/users/by-email")
    UserDto getUserByEmail(@RequestParam("email") String email);

    @GetMapping("/iam/users/{userId}")
    UserDto getUserById(@PathVariable("userId") String userId);

    @PostMapping("/iam/users/batch")
    List<UserDto> getUsersByIds(@RequestBody List<String> userIds);

    /**
     * Writes an audit-log entry. Authenticated via the shared {@code X-Internal-Key} header that
     * {@link com.civicdesk.citizen.config.FeignConfig} attaches to every Feign request.
     */
    @PostMapping("/audit/auditLogs")
    void createAuditLog(@RequestBody CreateAuditLogRequest request);
}
