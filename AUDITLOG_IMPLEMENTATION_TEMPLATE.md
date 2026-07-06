# AuditLog Implementation Template for CivicDesk Microservices

## Overview
AuditLog is a centralized audit logging system implemented in the **auth-service** that tracks all critical user actions across all CivicDesk modules. This template guides module teams on how to integrate AuditLog into their services.

---

## Architecture Overview

```
┌─────────────────────────────────────────┐
│   Your Module (Service Request, etc)    │
├─────────────────────────────────────────┤
│   Controller/Service Layer              │
│   ├─ Inject AuditService                │
│   ├─ Call auditService.log()            │
│   └─ Pass userId, action, module, ip    │
├─────────────────────────────────────────┤
│   Via API Gateway                       │
├─────────────────────────────────────────┤
│   Auth Service (Centralized)            │
│   ├─ AuditService (Business Logic)      │
│   ├─ AuditLog Entity (JPA/Hibernate)    │
│   ├─ AuditLogRepository (Data Access)   │
│   └─ AuditLogController (REST API)      │
├─────────────────────────────────────────┤
│   Database (Shared audit_log table)     │
└─────────────────────────────────────────┘
```

---

## 1. Core Components (Auth Service - Central)

### 1.1 AuditLog Entity
**Location:** `auth-service/src/main/java/com/civicdesk/auth/entity/AuditLog.java`

```java
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_userId",    columnList = "userId"),
    @Index(name = "idx_audit_action",    columnList = "action"),
    @Index(name = "idx_audit_module",    columnList = "module"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
public class AuditLog {
    @Id
    @GeneratedValue(generator = "auditIdSeq")
    @GenericGenerator(...)
    private String auditId;                 // Unique sequential ID
    
    @Column(name = "userId", nullable = false, length = 36)
    private String userId;                  // User who performed action
    
    @Column(nullable = false, length = 50)
    private String action;                  // What action (e.g., CREATE_PERMIT, LOGIN)
    
    @Column(nullable = false, length = 50)
    private String module;                  // Module name (e.g., PERMIT, SERVICE_REQUEST)
    
    @Column(name = "ipAddress", length = 45)
    private String ipAddress;               // Client IP address
    
    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime timestamp;        // Auto timestamp
}
```

### 1.2 Enums (Define Actions and Modules)
**Location:** `auth-service/src/main/java/com/civicdesk/auth/enums/`

#### AuditAction.java
```java
public enum AuditAction {
    // IAM
    LOGIN, LOGOUT, REGISTER, CREATE_USER, UPDATE_USER,
    
    // Permit Service Actions
    CREATE_PERMIT, RENEW_PERMIT, SCHEDULE_INSPECTION, SUBMIT_INSPECTION,
    PERMIT_DECISION,
    
    // Service Request Actions
    CREATE_SERVICE, UPDATE_SERVICE, SUBMIT_REQUEST, UPDATE_REQUEST_STATUS,
    
    // Grievance Service Actions
    CREATE_GRIEVANCE, UPDATE_GRIEVANCE, CLOSE_GRIEVANCE, RESOLVE_GRIEVANCE,
    
    // Works Service Actions
    ...
}
```

#### AuditModule.java
```java
public enum AuditModule {
    IAM,
    SERVICE_REQUEST,
    GRIEVANCE,
    PERMIT,
    WORKS
}
```

### 1.3 AuditService Interface & Implementation
**Location:** `auth-service/src/main/java/com/civicdesk/auth/service/AuditService.java`

```java
public interface AuditService {
    void log(String userId, String action, String module, String ip);
    PageResponse<AuditLogResponse> getAll(String userId, String action, String module, int page, int size);
    AuditLogResponse getById(String id);
}
```

**Implementation:**
```java
@Service
public class AuditServiceImpl implements AuditService {
    
    private final AuditLogRepository auditLogRepository;
    
    @Override
    public void log(String userId, String action, String module, String ip) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setModule(module);
        log.setIpAddress(ip);
        auditLogRepository.save(log);   // Async preferred for production
    }
    
    @Override
    public PageResponse<AuditLogResponse> getAll(String userId, String action, String module, 
                                                 int page, int size) {
        // Build dynamic specification for filtering
        Specification<AuditLog> spec = Specification.where(null);
        if (userId != null && !userId.isBlank()) {
            spec = spec.and(AuditLogSpecifications.hasUserId(userId.trim()));
        }
        if (action != null && !action.isBlank()) {
            spec = spec.and(AuditLogSpecifications.hasAction(action));
        }
        if (module != null && !module.isBlank()) {
            spec = spec.and(AuditLogSpecifications.hasModule(module));
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> logs = auditLogRepository.findAll(spec, pageable);
        return PageResponse.from(logs, AuditLogResponse::from);
    }
}
```

### 1.4 AuditLogRepository
**Location:** `auth-service/src/main/java/com/civicdesk/auth/repository/AuditLogRepository.java`

```java
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String>,
        JpaSpecificationExecutor<AuditLog> {
    
    Page<AuditLog> findByUserId(String userId, Pageable pageable);
    Page<AuditLog> findByModule(String module, Pageable pageable);
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
```

### 1.5 AuditLogController (For Retrieval & Admin)
**Location:** `auth-service/src/main/java/com/civicdesk/auth/controller/AuditLogController.java`

```java
@RestController
@RequestMapping("/audit/auditLogs")
public class AuditLogController {
    
    private final AuditService auditService;
    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADM', 'CO')")  // Admin only
    public ResponseEntity<ApiResponse> getAuditLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<AuditLogResponse> logs = auditService.getAll(userId, action, module, page, size);
        return ResponseEntity.ok(ApiResponse.data(logs));
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADM', 'CO')")  // Service-to-service calls
    public ResponseEntity<ApiResponse> createAuditLog(HttpServletRequest request,
                                                      @RequestBody CreateAuditLogRequest req) {
        auditService.log(req.getUserId(), req.getAction(), req.getModule(), req.getIpAddress());
        return ResponseEntity.ok(ApiResponse.of("Audit Log Added", null));
    }
}
```

---

## 2. How to Integrate AuditLog into Your Module

### Step 1: Add Required Enums to Auth Service
**Modify:** `auth-service/src/main/java/com/civicdesk/auth/enums/AuditAction.java`

Add your module's actions:
```java
public enum AuditAction {
    // ... existing actions ...
    
    // Your Service Actions
    YOUR_CREATE_ACTION,
    YOUR_UPDATE_ACTION,
    YOUR_DELETE_ACTION,
    // etc.
}
```

**Modify:** `auth-service/src/main/java/com/civicdesk/auth/enums/AuditModule.java`

```java
public enum AuditModule {
    IAM,
    SERVICE_REQUEST,
    GRIEVANCE,
    PERMIT,
    WORKS
    // YOUR_NEW_MODULE if needed
}
```

### Step 2: Setup Feign Client in Your Module (For Inter-Service Communication)

**Create:** `your-module/src/main/java/com/civicdesk/yourmodule/feign/AuditServiceClient.java`

```java
@FeignClient(name = "auth-service", url = "http://auth-service:8000")
public interface AuditServiceClient {
    
    @PostMapping("/audit/auditLogs")
    ResponseEntity<ApiResponse> createAuditLog(@RequestBody CreateAuditLogRequest request);
}
```

**Create:** `your-module/src/main/java/com/civicdesk/yourmodule/dto/CreateAuditLogRequest.java`

```java
public class CreateAuditLogRequest {
    private String userId;      // Required
    private String action;      // Required - from AuditAction enum
    private String module;      // Required - from AuditModule enum
    private String ipAddress;   // Required - client IP
    
    // Getters & Setters
}
```

### Step 3: Inject AuditService in Your Service Layer

**In your module's service class:**

```java
@Service
public class YourModuleService {
    
    private final AuditServiceClient auditServiceClient;
    
    public YourModuleService(AuditServiceClient auditServiceClient) {
        this.auditServiceClient = auditServiceClient;
    }
    
    public void createEntity(CreateEntityRequest request, String userId, String clientIp) {
        // Business logic
        entity = new YourEntity();
        entity.save();
        
        // Log audit action
        CreateAuditLogRequest auditReq = new CreateAuditLogRequest(
            userId,
            AuditAction.YOUR_CREATE_ACTION.name(),
            AuditModule.YOUR_MODULE.name(),
            clientIp
        );
        auditServiceClient.createAuditLog(auditReq);
    }
}
```

### Step 4: Inject in Controllers and Call During Requests

**In your module's controller:**

```java
@RestController
@RequestMapping("/api/entities")
public class YourModuleController {
    
    private final YourModuleService service;
    private final AuditServiceClient auditServiceClient;
    
    public YourModuleController(YourModuleService service, AuditServiceClient auditServiceClient) {
        this.service = service;
        this.auditServiceClient = auditServiceClient;
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse> createEntity(
            @RequestBody CreateEntityRequest request,
            HttpServletRequest httpRequest) {
        
        String userId = SecurityContextUtil.getCurrentUserId();
        String clientIp = ClientIpUtil.resolve(httpRequest);
        
        // Call service (which handles audit logging internally)
        service.createEntity(request, userId, clientIp);
        
        return ResponseEntity.status(201).body(ApiResponse.of("Entity created", null));
    }
}
```

---

## 3. Code Snippets to Use

### 3.1 In Controller Layer
```java
// Get current user and IP
String userId = SecurityContextUtil.getCurrentUserId();
String clientIp = ClientIpUtil.resolve(httpRequest);

// Log action
CreateAuditLogRequest auditReq = new CreateAuditLogRequest(
    userId,
    AuditAction.CREATE_PERMIT.name(),
    AuditModule.PERMIT.name(),
    clientIp
);
auditServiceClient.createAuditLog(auditReq);
```

### 3.2 In Service Layer (Better Practice)
```java
public void updateData(String id, UpdateRequest request, String userId, String clientIp) {
    // Update business logic
    data.update(request);
    dataRepository.save(data);
    
    // Log audit
    CreateAuditLogRequest auditReq = new CreateAuditLogRequest(
        userId,
        AuditAction.UPDATE_YOUR_ACTION.name(),
        AuditModule.YOUR_MODULE.name(),
        clientIp
    );
    auditServiceClient.createAuditLog(auditReq);
}
```

### 3.3 Async Audit Logging (Optional - For Performance)
```java
@Async
public void logActionAsync(String userId, String action, String module, String ip) {
    try {
        auditServiceClient.createAuditLog(
            new CreateAuditLogRequest(userId, action, module, ip)
        );
    } catch (Exception e) {
        logger.error("Failed to log audit action", e);
    }
}
```

---

## 4. Best Practices

### DO's ✅
1. **Always log critical actions** - Create, Update, Delete, Approve, Reject, etc.
2. **Include user context** - Always capture userId and clientIp
3. **Use consistent enums** - Use AuditAction and AuditModule enums
4. **Log at service layer** - This ensures logging happens regardless of entry point
5. **Use try-catch for audit** - Don't let audit failures break business logic
6. **Normalize values** - Trim and uppercase action/module before logging

### DON'Ts ❌
1. **Don't log sensitive data** - Avoid logging passwords, secrets, PII in audit logs
2. **Don't block main flow** - Use async where audit logging might slow down response
3. **Don't miss critical actions** - Approval, Status changes, Document uploads
4. **Don't hardcode action names** - Always use enums for consistency
5. **Don't fail the request if audit fails** - Log errors but don't throw exceptions

---

## 5. Querying Audit Logs (Admin Interface)

### API Endpoint
```
GET /audit/auditLogs?userId=USER_ID&action=LOGIN&module=IAM&page=0&size=20
```

**Parameters:**
- `userId` - Filter by user
- `action` - Filter by action (e.g., LOGIN, CREATE_PERMIT)
- `module` - Filter by module (e.g., PERMIT, SERVICE_REQUEST)
- `page` - Page number (0-indexed)
- `size` - Records per page

### Response Format
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "auditId": "10000001",
        "userId": "user-123",
        "action": "LOGIN",
        "module": "IAM",
        "ipAddress": "192.168.1.1",
        "timestamp": "2026-07-03T10:16:12"
      }
    ],
    "totalElements": 150,
    "totalPages": 8,
    "currentPage": 0,
    "pageSize": 20
  }
}
```

---

## 6. Database Schema

```sql
CREATE TABLE audit_log (
    auditId VARCHAR(36) PRIMARY KEY,
    userId VARCHAR(36) NOT NULL,
    action VARCHAR(50) NOT NULL,
    module VARCHAR(50) NOT NULL,
    ipAddress VARCHAR(45),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_audit_userId (userId),
    INDEX idx_audit_action (action),
    INDEX idx_audit_module (module),
    INDEX idx_audit_timestamp (timestamp)
);
```

---

## 7. Checklist for Module Teams

- [ ] Added new AuditAction enums to auth-service
- [ ] Updated AuditModule if needed
- [ ] Created AuditServiceClient Feign interface
- [ ] Created CreateAuditLogRequest DTO
- [ ] Injected AuditServiceClient in service layer
- [ ] Added audit logging to all critical business operations
- [ ] Passed userId and clientIp to service methods
- [ ] Tested audit logs are being captured
- [ ] Verified logs appear in admin dashboard
- [ ] Added error handling for audit logging failures

---

## 8. Example Implementation - Permit Service

### Complete Example:

```java
// EnumsPayloadAction.java
public enum AuditAction {
    CREATE_PERMIT,
    RENEW_PERMIT,
    SCHEDULE_INSPECTION,
    SUBMIT_INSPECTION,
    PERMIT_DECISION
}

// PermitService.java
@Service
public class PermitService {
    private final PermitRepository permitRepository;
    private final AuditServiceClient auditServiceClient;
    
    public void createPermit(CreatePermitRequest request, String userId, String clientIp) {
        // Create permit
        Permit permit = new Permit();
        permit.populate(request);
        permitRepository.save(permit);
        
        // Log audit
        auditServiceClient.createAuditLog(new CreateAuditLogRequest(
            userId,
            AuditAction.CREATE_PERMIT.name(),
            AuditModule.PERMIT.name(),
            clientIp
        ));
    }
}

// PermitController.java
@RestController
@RequestMapping("/api/permits")
public class PermitController {
    private final PermitService permitService;
    
    @PostMapping
    public ResponseEntity<ApiResponse> createPermit(
            @RequestBody CreatePermitRequest request,
            HttpServletRequest httpRequest) {
        String userId = SecurityContextUtil.getCurrentUserId();
        String clientIp = ClientIpUtil.resolve(httpRequest);
        
        permitService.createPermit(request, userId, clientIp);
        return ResponseEntity.status(201).body(ApiResponse.of("Permit created", null));
    }
}
```

---

## 9. Troubleshooting

| Issue | Solution |
|-------|----------|
| Audit logs not appearing | Check if AuditServiceClient is correctly configured and auth-service is running |
| Service-to-service auth failing | Ensure API Gateway routing is configured for auth-service |
| Null userId in logs | Verify SecurityContextUtil.getCurrentUserId() is called within authenticated context |
| Duplicate logs | Check if audit logging is happening in both controller and service |
| Performance degradation | Consider using @Async for audit logging |

---

## 10. Support & Questions

Contact the **Platform Team** or **Auth Service Owners** for:
- Adding new AuditAction enums
- Questions about audit log queries
- Issues with Feign client configuration
- Performance tuning for audit logging

---

**Template Version:** 1.0  
**Last Updated:** July 2026  
**Maintained By:** Platform Team
