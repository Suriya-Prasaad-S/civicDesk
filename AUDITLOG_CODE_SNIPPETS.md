# AuditLog - Ready-to-Use Code Snippets

This document contains copy-paste ready code snippets for implementing AuditLog in your module.

---

## 1. Feign Client Setup

### File: `src/main/java/com/civicdesk/yourmodule/feign/AuditServiceClient.java`

```java
package com.civicdesk.yourmodule.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "auth-service",
    url = "${feign.auth-service.url:http://auth-service:8000}"
)
public interface AuditServiceClient {

    @PostMapping("/audit/auditLogs")
    ResponseEntity<?> createAuditLog(@RequestBody CreateAuditLogRequest request);
}
```

---

## 2. DTO Classes

### File: `src/main/java/com/civicdesk/yourmodule/dto/CreateAuditLogRequest.java`

```java
package com.civicdesk.yourmodule.dto;

/**
 * Request DTO for creating audit log entries.
 * This is sent to the centralized audit service.
 */
public class CreateAuditLogRequest {

    private String userId;      // User who performed action (required)
    private String action;      // Action name (required) - use AuditAction enum
    private String module;      // Module name (required) - use AuditModule enum
    private String ipAddress;   // Client IP address (required)

    // Constructors
    public CreateAuditLogRequest() {
    }

    public CreateAuditLogRequest(String userId, String action, String module, String ipAddress) {
        this.userId = userId;
        this.action = action;
        this.module = module;
        this.ipAddress = ipAddress;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public String getModule() {
        return module;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    // Setters
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public String toString() {
        return "CreateAuditLogRequest{" +
                "userId='" + userId + '\'' +
                ", action='" + action + '\'' +
                ", module='" + module + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}
```

### File: `src/main/java/com/civicdesk/yourmodule/enums/YourAuditAction.java`

```java
package com.civicdesk.yourmodule.enums;

/**
 * ⚠️ NOTE: This is a local reference only.
 * Master AuditAction enum is in auth-service.
 * Use action names exactly as defined there.
 */
public enum YourAuditAction {
    // Your Module Actions (Update these based on auth-service AuditAction enum)
    CREATE_YOUR_ENTITY("CREATE_YOUR_ENTITY"),
    UPDATE_YOUR_ENTITY("UPDATE_YOUR_ENTITY"),
    DELETE_YOUR_ENTITY("DELETE_YOUR_ENTITY"),
    SUBMIT_YOUR_REQUEST("SUBMIT_YOUR_REQUEST"),
    APPROVE_YOUR_REQUEST("APPROVE_YOUR_REQUEST"),
    REJECT_YOUR_REQUEST("REJECT_YOUR_REQUEST");

    private final String value;

    YourAuditAction(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
```

### File: `src/main/java/com/civicdesk/yourmodule/enums/AuditModuleConstant.java`

```java
package com.civicdesk.yourmodule.enums;

/**
 * Constants for audit module names.
 * These must match the auth-service AuditModule enum.
 */
public class AuditModuleConstant {
    public static final String IAM = "IAM";
    public static final String SERVICE_REQUEST = "SERVICE_REQUEST";
    public static final String GRIEVANCE = "GRIEVANCE";
    public static final String PERMIT = "PERMIT";
    public static final String WORKS = "WORKS";
    public static final String YOUR_MODULE = "YOUR_MODULE";  // Add your module
}
```

---

## 3. Utility Classes

### File: `src/main/java/com/civicdesk/yourmodule/util/ClientIpUtil.java`

```java
package com.civicdesk.yourmodule.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility to extract client IP address from HTTP request.
 */
public class ClientIpUtil {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_MOZ_FORWARDED_FOR",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_CLIENT_IP",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    public static String resolve(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"UNKNOWN".equalsIgnoreCase(ip)) {
                // Handle multiple IPs in X-Forwarded-For (take first)
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        // Fallback to remote address
        return request.getRemoteAddr();
    }
}
```

### File: `src/main/java/com/civicdesk/yourmodule/util/SecurityContextUtil.java`

```java
package com.civicdesk.yourmodule.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility to extract current user information from Spring Security context.
 */
public class SecurityContextUtil {

    /**
     * Get current authenticated user's ID.
     * @return userId from JWT token
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "UNKNOWN";
        }
        // Assuming the principal is a custom UserDetails object with getId()
        // Adjust based on your security implementation
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            return (String) principal;
        }
        return principal.toString();
    }

    /**
     * Get current user's roles/authorities.
     */
    public static String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "ANONYMOUS";
        }
        return authentication.getAuthorities().toString();
    }

    /**
     * Check if user has specific role.
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role));
    }
}
```

---

## 4. Service Layer Implementation

### File: `src/main/java/com/civicdesk/yourmodule/service/YourEntityService.java`

```java
package com.civicdesk.yourmodule.service;

import com.civicdesk.yourmodule.dto.CreateAuditLogRequest;
import com.civicdesk.yourmodule.dto.CreateYourEntityRequest;
import com.civicdesk.yourmodule.entity.YourEntity;
import com.civicdesk.yourmodule.feign.AuditServiceClient;
import com.civicdesk.yourmodule.repository.YourEntityRepository;
import com.civicdesk.yourmodule.enums.AuditModuleConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service layer with integrated audit logging.
 * All critical business operations log audit trail.
 */
@Service
@Slf4j
public class YourEntityService {

    private final YourEntityRepository repository;
    private final AuditServiceClient auditServiceClient;

    public YourEntityService(
            YourEntityRepository repository,
            AuditServiceClient auditServiceClient) {
        this.repository = repository;
        this.auditServiceClient = auditServiceClient;
    }

    /**
     * Create entity and log to audit trail.
     */
    public YourEntity createEntity(
            CreateYourEntityRequest request,
            String userId,
            String clientIp) {

        // Business logic
        YourEntity entity = new YourEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        YourEntity saved = repository.save(entity);

        // Log audit action (don't block if audit fails)
        logAudit(userId, "CREATE_YOUR_ENTITY", AuditModuleConstant.YOUR_MODULE, clientIp);

        return saved;
    }

    /**
     * Update entity and log to audit trail.
     */
    public YourEntity updateEntity(
            String id,
            CreateYourEntityRequest request,
            String userId,
            String clientIp) {

        // Business logic
        YourEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entity not found"));
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        YourEntity updated = repository.save(entity);

        // Log audit action
        logAudit(userId, "UPDATE_YOUR_ENTITY", AuditModuleConstant.YOUR_MODULE, clientIp);

        return updated;
    }

    /**
     * Delete entity and log to audit trail.
     */
    public void deleteEntity(String id, String userId, String clientIp) {
        // Business logic
        repository.deleteById(id);

        // Log audit action
        logAudit(userId, "DELETE_YOUR_ENTITY", AuditModuleConstant.YOUR_MODULE, clientIp);
    }

    /**
     * Approve entity and log to audit trail.
     */
    public YourEntity approveEntity(String id, String userId, String clientIp) {
        YourEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entity not found"));
        
        entity.setStatus("APPROVED");
        YourEntity updated = repository.save(entity);

        // Log audit action
        logAudit(userId, "APPROVE_YOUR_REQUEST", AuditModuleConstant.YOUR_MODULE, clientIp);

        return updated;
    }

    /**
     * Private helper method to log audit actions.
     * Uses try-catch to ensure audit failures don't break business logic.
     */
    private void logAudit(String userId, String action, String module, String ipAddress) {
        try {
            CreateAuditLogRequest auditRequest = new CreateAuditLogRequest(
                    userId,
                    action,
                    module,
                    ipAddress
            );
            auditServiceClient.createAuditLog(auditRequest);
            log.info("Audit logged: userId={}, action={}, module={}", userId, action, module);
        } catch (Exception e) {
            // Log error but don't throw - never block business logic for audit
            log.error("Failed to create audit log: userId={}, action={}, module={}", 
                    userId, action, module, e);
        }
    }

    /**
     * Alternative: Async audit logging (Optional - for performance).
     */
    // @Async
    // private void logAuditAsync(String userId, String action, String module, String ipAddress) {
    //     try {
    //         logAudit(userId, action, module, ipAddress);
    //     } catch (Exception e) {
    //         log.error("Async audit log failed", e);
    //     }
    // }
}
```

---

## 5. Controller Layer Implementation

### File: `src/main/java/com/civicdesk/yourmodule/controller/YourEntityController.java`

```java
package com.civicdesk.yourmodule.controller;

import com.civicdesk.yourmodule.dto.ApiResponse;
import com.civicdesk.yourmodule.dto.CreateYourEntityRequest;
import com.civicdesk.yourmodule.entity.YourEntity;
import com.civicdesk.yourmodule.service.YourEntityService;
import com.civicdesk.yourmodule.util.ClientIpUtil;
import com.civicdesk.yourmodule.util.SecurityContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller with audit logging integration.
 */
@RestController
@RequestMapping("/api/entities")
@Slf4j
public class YourEntityController {

    private final YourEntityService service;

    public YourEntityController(YourEntityService service) {
        this.service = service;
    }

    /**
     * Create new entity with audit logging.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse> createEntity(
            @Valid @RequestBody CreateYourEntityRequest request,
            HttpServletRequest httpRequest) {

        String userId = SecurityContextUtil.getCurrentUserId();
        String clientIp = ClientIpUtil.resolve(httpRequest);

        log.info("Creating entity: userId={}, ip={}", userId, clientIp);

        YourEntity entity = service.createEntity(request, userId, clientIp);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Entity created successfully", entity));
    }

    /**
     * Update existing entity with audit logging.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse> updateEntity(
            @PathVariable String id,
            @Valid @RequestBody CreateYourEntityRequest request,
            HttpServletRequest httpRequest) {

        String userId = SecurityContextUtil.getCurrentUserId();
        String clientIp = ClientIpUtil.resolve(httpRequest);

        log.info("Updating entity: id={}, userId={}, ip={}", id, userId, clientIp);

        YourEntity entity = service.updateEntity(id, request, userId, clientIp);

        return ResponseEntity.ok(ApiResponse.of("Entity updated successfully", entity));
    }

    /**
     * Delete entity with audit logging.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse> deleteEntity(
            @PathVariable String id,
            HttpServletRequest httpRequest) {

        String userId = SecurityContextUtil.getCurrentUserId();
        String clientIp = ClientIpUtil.resolve(httpRequest);

        log.info("Deleting entity: id={}, userId={}, ip={}", id, userId, clientIp);

        service.deleteEntity(id, userId, clientIp);

        return ResponseEntity.ok(ApiResponse.of("Entity deleted successfully", null));
    }

    /**
     * Approve entity with audit logging.
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ResponseEntity<ApiResponse> approveEntity(
            @PathVariable String id,
            HttpServletRequest httpRequest) {

        String userId = SecurityContextUtil.getCurrentUserId();
        String clientIp = ClientIpUtil.resolve(httpRequest);

        log.info("Approving entity: id={}, userId={}, ip={}", id, userId, clientIp);

        YourEntity entity = service.approveEntity(id, userId, clientIp);

        return ResponseEntity.ok(ApiResponse.of("Entity approved successfully", entity));
    }
}
```

---

## 6. Configuration

### File: `src/main/resources/application.yml`

```yaml
# Feign client configuration
feign:
  auth-service:
    url: http://auth-service:8000

# Circuit breaker for audit service (if using Resilience4j)
resilience4j:
  circuitbreaker:
    instances:
      auth-service:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50.0
        waitDurationInOpenState: 60000
        permittedNumberOfCallsInHalfOpenState: 3
        slowCallRateThreshold: 100.0
        slowCallDurationThreshold: 60000ms
        recordExceptions:
          - java.io.IOException
          - java.net.ConnectException

  retry:
    instances:
      auth-service:
        maxAttempts: 3
        waitDuration: 1000
        retryExceptions:
          - java.io.IOException
          - java.net.ConnectException
```

### File: `pom.xml`

```xml
<!-- Add to dependencies -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>

<!-- Optional: Circuit breaker for resilience -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot2</artifactId>
</dependency>

<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-circuitbreaker</artifactId>
</dependency>
```

### File: `src/main/java/com/civicdesk/yourmodule/config/FeignConfig.java` (Optional)

```java
package com.civicdesk.yourmodule.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;  // Or BASIC, HEADERS, NONE
    }
}
```

---

## 7. Spring Boot Main Class

### File: `src/main/java/com/civicdesk/yourmodule/YourModuleApplication.java`

```java
package com.civicdesk.yourmodule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableFeignClients  // Enable Feign client scanning
@EnableAsync         // Optional: For async audit logging
public class YourModuleApplication {

    public static void main(String[] args) {
        SpringApplication.run(YourModuleApplication.class, args);
    }
}
```

---

## 8. Error Handling

### File: `src/main/java/com/civicdesk/yourmodule/exception/AuditLoggingException.java`

```java
package com.civicdesk.yourmodule.exception;

/**
 * Exception for audit logging failures.
 * Should be caught and logged but never thrown to caller.
 */
public class AuditLoggingException extends RuntimeException {
    
    public AuditLoggingException(String message) {
        super(message);
    }
    
    public AuditLoggingException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

## 9. Testing

### File: `src/test/java/com/civicdesk/yourmodule/service/YourEntityServiceTest.java`

```java
package com.civicdesk.yourmodule.service;

import com.civicdesk.yourmodule.dto.CreateAuditLogRequest;
import com.civicdesk.yourmodule.dto.CreateYourEntityRequest;
import com.civicdesk.yourmodule.entity.YourEntity;
import com.civicdesk.yourmodule.feign.AuditServiceClient;
import com.civicdesk.yourmodule.repository.YourEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class YourEntityServiceTest {

    private YourEntityService service;

    @Mock
    private YourEntityRepository repository;

    @Mock
    private AuditServiceClient auditServiceClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new YourEntityService(repository, auditServiceClient);
    }

    @Test
    void testCreateEntityLogsAudit() {
        // Arrange
        CreateYourEntityRequest request = new CreateYourEntityRequest();
        request.setName("Test Entity");
        YourEntity entity = new YourEntity();
        
        when(repository.save(any())).thenReturn(entity);
        when(auditServiceClient.createAuditLog(any())).thenReturn(ResponseEntity.ok().build());

        // Act
        service.createEntity(request, "user-123", "192.168.1.1");

        // Assert
        verify(repository, times(1)).save(any());
        verify(auditServiceClient, times(1)).createAuditLog(any(CreateAuditLogRequest.class));
    }

    @Test
    void testCreateEntityDoesNotFailIfAuditFails() {
        // Arrange
        CreateYourEntityRequest request = new CreateYourEntityRequest();
        request.setName("Test Entity");
        YourEntity entity = new YourEntity();
        
        when(repository.save(any())).thenReturn(entity);
        when(auditServiceClient.createAuditLog(any()))
                .thenThrow(new RuntimeException("Audit service down"));

        // Act & Assert (should not throw)
        service.createEntity(request, "user-123", "192.168.1.1");
        
        verify(repository, times(1)).save(any());
    }
}
```

---

## 10. Deployment Checklist

- [ ] Feign client configured for auth-service URL
- [ ] AuditAction enums added to auth-service
- [ ] AuditModule enum updated in auth-service
- [ ] CreateAuditLogRequest DTO created
- [ ] AuditServiceClient Feign interface created
- [ ] Service layer methods inject AuditServiceClient
- [ ] Controllers pass userId and clientIp to service
- [ ] Error handling ensures audit failures don't break business logic
- [ ] Database indices created on audit_log table
- [ ] Tested in dev environment
- [ ] Tested failover (audit service down scenario)
- [ ] Documented in team wiki

---

**Last Updated:** July 2026  
**Version:** 1.0
