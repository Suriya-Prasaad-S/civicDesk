# AuditLog Quick Start Guide - TL;DR Version

## 5-Minute Quick Start for Module Teams

---

## What is AuditLog?

**Centralized audit trail system** that tracks all critical user actions across CivicDesk microservices.

**Location:** auth-service (centralized)  
**Database:** `audit_log` table  
**Purpose:** Compliance, debugging, user activity tracking

---

## High-Level Architecture

```
Your Service  →  Call AuditService  →  Auth Service  →  Database
  (Permit,          via Feign            (Central)
  Grievance,    CreateAuditLogRequest                   audit_log
  etc.)
```

---

## 3 Simple Steps to Integrate

### Step 1: Create Feign Client
```java
@FeignClient(name = "auth-service", url = "http://auth-service:8000")
public interface AuditServiceClient {
    @PostMapping("/audit/auditLogs")
    ResponseEntity<?> createAuditLog(@RequestBody CreateAuditLogRequest request);
}
```

### Step 2: Inject in Service & Log Actions
```java
@Service
public class YourService {
    private final AuditServiceClient auditClient;
    
    public void doSomething(String userId, String clientIp) {
        // Your business logic
        
        // Log audit
        auditClient.createAuditLog(new CreateAuditLogRequest(
            userId,
            "YOUR_ACTION",           // Action enum value
            "YOUR_MODULE",           // Module name
            clientIp
        ));
    }
}
```

### Step 3: Pass User & IP from Controller
```java
@PostMapping
public ResponseEntity<?> create(@RequestBody Request req, HttpServletRequest http) {
    String userId = SecurityContextUtil.getCurrentUserId();
    String ip = ClientIpUtil.resolve(http);
    
    service.doSomething(userId, ip);
    return ResponseEntity.ok(ApiResponse.of("Done", null));
}
```

---

## Required Information for Each Audit Log

| Field | Example | Required |
|-------|---------|----------|
| `userId` | "user-123" | ✅ Yes |
| `action` | "CREATE_PERMIT" | ✅ Yes |
| `module` | "PERMIT" | ✅ Yes |
| `ipAddress` | "192.168.1.1" | ✅ Yes |
| `timestamp` | Auto-generated | No |

---

## What Actions to Log

### CREATE, UPDATE, DELETE ✅
```java
auditClient.createAuditLog(new CreateAuditLogRequest(
    userId, "CREATE_PERMIT", "PERMIT", ip
));
```

### APPROVE, REJECT ✅
```java
auditClient.createAuditLog(new CreateAuditLogRequest(
    userId, "APPROVE_REQUEST", "SERVICE_REQUEST", ip
));
```

### STATUS CHANGES ✅
```java
auditClient.createAuditLog(new CreateAuditLogRequest(
    userId, "UPDATE_STATUS", "GRIEVANCE", ip
));
```

### LOGIN, LOGOUT ✅
```java
auditClient.createAuditLog(new CreateAuditLogRequest(
    userId, "LOGIN", "IAM", ip
));
```

### Avoid Logging ❌
- Read-only operations (GET)
- Administrative tasks that aren't user-driven
- System background jobs (unless critical)

---

## Utility Methods You Already Have

### Get Current User
```java
String userId = SecurityContextUtil.getCurrentUserId();
```

### Get Client IP
```java
String ip = ClientIpUtil.resolve(httpRequest);
```

### Available Actions (Add New Ones to Auth Service)
```java
// In auth-service: AuditAction enum
CREATE_PERMIT, RENEW_PERMIT, APPROVE_REQUEST, REJECT_REQUEST, 
UPDATE_STATUS, CLOSE_TICKET, ASSIGN_OFFICER, etc.
```

### Available Modules
```java
// In auth-service: AuditModule enum
IAM, PERMIT, SERVICE_REQUEST, GRIEVANCE, WORKS
```

---

## Common Mistakes to Avoid ❌

1. **Don't forget userId**  
   ❌ Bad: `createAuditLog(null, "ACTION", "MODULE", ip)`  
   ✅ Good: `createAuditLog(userId, "ACTION", "MODULE", ip)`

2. **Don't use wrong action names**  
   ❌ Bad: `"create_permit"` (lowercase)  
   ✅ Good: `"CREATE_PERMIT"` (from enum)

3. **Don't block business logic for audit failure**  
   ❌ Bad: `throw new Exception("Audit failed")`  
   ✅ Good: `catch and log, but continue`

4. **Don't log in controller only**  
   ❌ Bad: Put all audit logging in controller  
   ✅ Good: Put it in service layer (reusable)

5. **Don't forget to enable Feign clients**  
   ❌ Bad: Forget `@EnableFeignClients`  
   ✅ Good: Add to main class

---

## Example Implementation (Copy-Paste Ready)

### Service Class
```java
@Service
@Slf4j
public class PermitService {
    private final AuditServiceClient auditClient;
    
    public void createPermit(CreatePermitRequest req, String userId, String ip) {
        // Create permit
        permitRepo.save(new Permit(req));
        
        // Log audit (wrapped in try-catch)
        try {
            auditClient.createAuditLog(new CreateAuditLogRequest(
                userId, "CREATE_PERMIT", "PERMIT", ip
            ));
        } catch (Exception e) {
            log.error("Audit log failed", e);  // Log but don't throw
        }
    }
}
```

### Controller Class
```java
@RestController
@RequestMapping("/api/permits")
public class PermitController {
    private final PermitService service;
    
    @PostMapping
    public ResponseEntity<?> createPermit(
            @RequestBody CreatePermitRequest req,
            HttpServletRequest http) {
        
        service.createPermit(
            req,
            SecurityContextUtil.getCurrentUserId(),
            ClientIpUtil.resolve(http)
        );
        
        return ResponseEntity.status(201)
            .body(ApiResponse.of("Permit created", null));
    }
}
```

---

## Querying Audit Logs (Admin Only)

### API Endpoint
```
GET /audit/auditLogs?userId=user-123&action=CREATE_PERMIT&module=PERMIT&page=0&size=20
```

### Response
```json
{
  "data": {
    "content": [
      {
        "auditId": "10000001",
        "userId": "user-123",
        "action": "CREATE_PERMIT",
        "module": "PERMIT",
        "ipAddress": "192.168.1.1",
        "timestamp": "2026-07-03T10:16:12"
      }
    ],
    "totalElements": 150,
    "totalPages": 8
  }
}
```

---

## Checklist Before Deployment

- [ ] Feign client created and configured
- [ ] AuditServiceClient injected in service layer
- [ ] All critical actions log audit trail
- [ ] userId and IP passed from controller to service
- [ ] Error handling for audit failures in place
- [ ] New AuditAction enums added to auth-service
- [ ] Tested: Can create audit logs successfully
- [ ] Tested: Service continues even if audit fails
- [ ] Code reviewed by team

---

## Need Help?

| Problem | Solution |
|---------|----------|
| Audit logs not appearing | Check if auth-service is running, check logs |
| NullPointerException on userId | Ensure controller is authenticated before calling service |
| Feign client not found | Add `@EnableFeignClients` to main class |
| Connection timeout | Check auth-service URL in application.yml |
| Action/Module not valid | Make sure you're using enum values, not hardcoded strings |

---

## Files to Create per Module

```
your-module/src/main/java/com/civicdesk/yourmodule/
├── feign/
│   └── AuditServiceClient.java         ← Create this
├── dto/
│   └── CreateAuditLogRequest.java      ← Create this
├── util/
│   ├── ClientIpUtil.java               ← Copy from template
│   └── SecurityContextUtil.java        ← Copy from template
├── service/
│   └── YourService.java                ← Add audit logging
└── controller/
    └── YourController.java             ← Pass userId & IP
```

---

## Minimal Code Example

```java
// Step 1: Create client interface
@FeignClient(name = "auth-service", url = "http://auth-service:8000")
public interface AuditClient {
    @PostMapping("/audit/auditLogs")
    ResponseEntity<?> log(@RequestBody CreateAuditLogRequest req);
}

// Step 2: Use in service
@Service
public class MyService {
    @Autowired AuditClient auditClient;
    
    public void doAction(String userId, String ip) {
        // business logic...
        auditClient.log(new CreateAuditLogRequest(userId, "DO_ACTION", "MY_MODULE", ip));
    }
}

// Step 3: Call from controller
@PostMapping
public ResponseEntity<?> endpoint(@RequestBody MyRequest req, HttpServletRequest http) {
    String userId = SecurityContextUtil.getCurrentUserId();
    String ip = ClientIpUtil.resolve(http);
    service.doAction(userId, ip);
    return ResponseEntity.ok("Done");
}
```

---

## Pro Tips 💡

1. **Use @Async for non-critical audit**
   ```java
   @Async
   public void logAuditAsync(String userId, String action, String module, String ip) {
       auditClient.log(new CreateAuditLogRequest(userId, action, module, ip));
   }
   ```

2. **Create a helper method in your service**
   ```java
   private void audit(String userId, String action, String ip) {
       try {
           auditClient.log(new CreateAuditLogRequest(userId, action, "MY_MODULE", ip));
       } catch (Exception e) {
           log.error("Audit failed", e);
       }
   }
   ```

3. **Batch audit logs for performance (if using message queue)**
   ```java
   // Send to Kafka/RabbitMQ, async processor handles batching
   ```

4. **Index important queries**
   ```java
   // Already done in AuditLog entity - indexes on userId, action, module, timestamp
   ```

---

**Quick Reference Card:** Print this section and keep at your desk!

```
════════════════════════════════════════
AUDITLOG - QUICK REFERENCE
════════════════════════════════════════

1. Create AuditServiceClient (Feign)
2. Inject in Service layer
3. Call: auditClient.log(userId, action, module, ip)
4. Pass userId & ip from Controller
5. Wrap in try-catch (don't block business logic)
6. Test, deploy, done!

REQUIRED: @EnableFeignClients on main class

SEE ALSO: AUDITLOG_IMPLEMENTATION_TEMPLATE.md
          AUDITLOG_CODE_SNIPPETS.md
════════════════════════════════════════
```

---

**Version:** 1.0  
**Last Updated:** July 2026  
**Contact:** Platform Team
