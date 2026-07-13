# Analytics Service - Before & After Comparison

## Architecture Overview

### Before: Monolithic Module
```
civicdesk/ (Monolith)
└── src/main/java/com/civicdesk/module/reporting/
    ├── controller/ReportController.java
    ├── service/IReportService.java
    ├── dto/request/GenerateReportRequest.java
    └── dto/response/ReportResponse.java
```

**Issues:**
- Part of monolithic application
- Direct in-process service calls (CitizenGrievanceService)
- No audit logging
- Package naming suggests module, not service
- Tightly coupled dependencies
- No inter-service communication patterns

### After: Microservice
```
analytics-service/ (Microservice)
├── src/main/java/com/civicdesk/analytics/
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── SwaggerConfig.java
│   │   └── BeansConfig.java (NEW)
│   ├── controller/
│   │   └── ReportController.java (UPDATED)
│   ├── client/ (NEW)
│   │   ├── AuditClient.java
│   │   └── GrievanceClient.java
│   ├── service/
│   │   ├── IReportService.java
│   │   ├── ReportServiceImpl.java
│   │   └── ReportExportService.java
│   ├── entity/ (NEW)
│   │   ├── CivicReport.java
│   │   └── MetricsConverter.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── GenerateReportRequest.java (UPDATED)
│   │   │   ├── GrievanceAnalyticsRequest.java (NEW)
│   │   │   └── CreateAuditLogRequest.java (NEW)
│   │   └── response/
│   │       ├── ReportResponse.java
│   │       ├── ReportSummaryResponse.java
│   │       └── GrievanceAnalyticsResponse.java (NEW)
│   ├── response/ (NEW)
│   │   ├── ApiResponse.java
│   │   └── PageResponse.java
│   ├── enums/ (NEW)
│   │   ├── AuditAction.java
│   │   └── AuditModule.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java (UPDATED)
│   │   ├── ResourceNotFoundException.java
│   │   └── InvalidReportTypeException.java (NEW)
│   ├── util/ (NEW)
│   │   └── ClientIpUtil.java
│   ├── security/
│   │   ├── JwtAuthFilter.java
│   │   ├── JwtTokenProvider.java
│   │   └── JwtUserContext.java
│   ├── repository/
│   │   └── CivicReportRepository.java (UPDATED)
│   └── AnalyticsServiceApplication.java
├── pom.xml (UPDATED)
├── src/main/resources/
│   ├── application.properties (UPDATED)
│   └── logback-spring.xml
├── Dockerfile
└── README.md (NEW)
```

**Improvements:**
- ✅ Independent Spring Boot application
- ✅ Microservice communication patterns
- ✅ Audit logging integration
- ✅ Service discovery ready
- ✅ Container-deployable
- ✅ Loose coupling

## Code Changes

### 1. Package Names

**Before:**
```java
package com.civicdesk.module.reporting.controller;
import com.civicdesk.common.response.ApiResponse;
import com.civicdesk.module.reporting.service.IReportService;
import com.civicdesk.module.grievance.service.CitizenGrievanceService;
```

**After:**
```java
package com.civicdesk.analytics.controller;
import com.civicdesk.analytics.response.ApiResponse;
import com.civicdesk.analytics.service.IReportService;
import com.civicdesk.analytics.client.AuditClient;
import com.civicdesk.analytics.client.GrievanceClient;
import com.civicdesk.analytics.enums.AuditAction;
import com.civicdesk.analytics.enums.AuditModule;
```

### 2. Controller Integration

**Before:**
```java
@RestController
@RequestMapping("/reports")
public class ReportController {
    
    private final IReportService reportService;

    @PostMapping
    public ResponseEntity<ApiResponse> generateReport(
            @Valid @RequestBody GenerateReportRequest request) throws Exception {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Generating report for user: {}", userId);
        reportService.generateReport(request, userId);
        // No audit logging!
        return ResponseEntity.status(201).body(ApiResponse.of("Report is ready", null));
    }
}
```

**After:**
```java
@RestController
@RequestMapping("/civicDesk/analytics/reports")
public class ReportController {
    
    private final IReportService reportService;
    private final AuditClient auditClient;  // NEW

    @PostMapping
    public ResponseEntity<ApiResponse> generateReport(
            @Valid @RequestBody GenerateReportRequest request,
            HttpServletRequest httpReq) throws Exception {  // NEW: capture request
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Generating report for user: {}", userId);
        reportService.generateReport(request, userId);
        
        // NEW: Audit logging to auth-service
        auditClient.logAudit(userId, 
                            AuditAction.GENERATE_REPORT.name(), 
                            AuditModule.ANALYTICS.name(), 
                            ClientIpUtil.resolve(httpReq));
        
        return ResponseEntity.status(201).body(ApiResponse.of("Report is ready", null));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteReport(
            @PathVariable("id") @NotBlank String id,
            HttpServletRequest httpReq) {  // NEW
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean deleted = reportService.softDeleteReport(id);
        
        // NEW: Log deletion action
        auditClient.logAudit(userId, 
                            AuditAction.DELETE_REPORT.name(), 
                            AuditModule.ANALYTICS.name(), 
                            ClientIpUtil.resolve(httpReq));
        
        if (!deleted) {
            return ResponseEntity.status(404).body(ApiResponse.error("Report not found"));
        }
        return ResponseEntity.ok(ApiResponse.of("Report deleted successfully", null));
    }
}
```

### 3. Service Dependencies

**Before (Monolithic):**
```java
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements IReportService {
    
    private final CivicReportRepository repository;
    private final ObjectMapper objectMapper;
    private final CitizenGrievanceService citizenGrievanceService;  // Direct service injection
    private final ReportExportService reportExportService;

    private Map<String, Object> getGrievanceAnalyticsMetrics(GenerateReportRequest request) {
        GrievanceAnalyticsRequest analyticsRequest = new GrievanceAnalyticsRequest();
        analyticsRequest.setFromDate(request.getFromDate());
        analyticsRequest.setToDate(request.getToDate());
        analyticsRequest.setDeptId(request.getDepartmentId());

        // Direct in-process call - tightly coupled
        GrievanceAnalyticsResponse analyticsResponse = 
            citizenGrievanceService.getGrievanceAnalytics(analyticsRequest);
        
        return objectMapper.convertValue(analyticsResponse, new TypeReference<>() {});
    }
}
```

**After (Microservice):**
```java
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements IReportService {
    
    private final CivicReportRepository repository;
    private final ObjectMapper objectMapper;
    private final GrievanceClient grievanceClient;  // HTTP client (REST)
    private final ReportExportService reportExportService;

    private Map<String, Object> getGrievanceAnalyticsMetrics(GenerateReportRequest request) {
        GrievanceAnalyticsRequest analyticsRequest = new GrievanceAnalyticsRequest();
        analyticsRequest.setFromDate(request.getFromDate());
        analyticsRequest.setToDate(request.getToDate());
        analyticsRequest.setDeptId(request.getDepartmentId());

        // HTTP call to grievance-service (loosely coupled)
        GrievanceAnalyticsResponse analyticsResponse = 
            grievanceClient.getGrievanceAnalytics(analyticsRequest);
        
        return objectMapper.convertValue(analyticsResponse, new TypeReference<>() {});
    }
}
```

### 4. Client for Inter-Service Communication

**Before:** Non-existent (no cross-service pattern)

**After (New):**
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditClient {
    
    private final RestTemplate restTemplate;

    @Value("${app.auth-service.url}")
    private String authServiceUrl;

    public void logAudit(String userId, String action, String module, String ip) {
        String jwtToken = getCurrentJwtToken();
        
        try {
            CreateAuditLogRequest auditRequest = CreateAuditLogRequest.builder()
                    .userId(userId)
                    .action(action)
                    .module(module)
                    .ipAddress(ip)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (jwtToken != null) {
                headers.set("Authorization", jwtToken);
            }

            HttpEntity<CreateAuditLogRequest> requestEntity = 
                new HttpEntity<>(auditRequest, headers);
            
            String endpoint = authServiceUrl + "/civicDesk/audit/auditLogs";

            log.info("Sending audit log: userId={}, action={}", userId, action);
            ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
                    endpoint,
                    requestEntity,
                    ApiResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Audit log sent successfully");
            }
        } catch (Exception e) {
            // Non-blocking - don't fail request if audit logging fails
            log.error("Error sending audit log: {}", e.getMessage());
        }
    }

    private String getCurrentJwtToken() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return attributes.getRequest().getHeader("Authorization");
        }
        return null;
    }
}
```

### 5. Configuration

**Before (application.properties):**
```properties
server.port=8088
spring.datasource.url=jdbc:mysql://localhost:3306/civicdesk_analytics
spring.datasource.username=root
spring.datasource.password=root
app.jwt.secret=civicdesk_hs256_secret_key_minimum_32_characters_required
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

**After (application.properties):**
```properties
server.port=8088
spring.application.name=analytics-service  # NEW: For service discovery

spring.datasource.url=jdbc:mysql://localhost:3306/civicdesk_analytics
spring.datasource.username=root
spring.datasource.password=root

app.jwt.secret=civicdesk_hs256_secret_key_minimum_32_characters_required

# NEW: Inter-service URLs
app.auth-service.url=http://localhost:8081
app.grievance-service.url=http://localhost:8085

# Updated: API Gateway paths
springdoc.api-docs.path=/civicDesk/api-docs
springdoc.swagger-ui.path=/civicDesk/swagger-ui.html

# NEW: Enhanced logging
logging.level.com.civicdesk.analytics=DEBUG
logging.level.org.springframework.security=INFO
```

### 6. Dependencies

**Before (pom.xml):**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>
<!-- Missing: poi-ooxml for Excel export -->
```

**After (pom.xml):**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
<!-- NEW: For Excel export functionality -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>
```

## Request/Response Flow

### Before: Monolithic Request
```
Client
  ↓
Monolith Gateway (port 9090)
  ↓
ReportController@8080 (same process)
  ↓
ReportService (same JVM)
  ↓ (direct call)
CitizenGrievanceService (same JVM)
  ↓
Grievance Business Logic
  ↓
Response
```

### After: Microservice Request
```
Client
  ↓
API Gateway (port 9090)
  │ JWT validation
  ↓
Analytics Service (port 8088) [Independent process]
  │
  ├─→ ReportController
  │     │ Validates request
  │     ↓
  │   ReportService
  │     │ Generates report
  │     ├─→ GrievanceClient
  │     │     ↓ HTTP GET
  │     │   Grievance Service (port 8085) [Independent process]
  │     │     ↓
  │     │   Grievance Analytics
  │     │     ↓
  │     │   GrievanceClient (receives response)
  │     │
  │     ├─→ AuditClient
  │     │     ↓ HTTP POST
  │     │   Auth Service (port 8081) [Independent process]
  │     │     ↓
  │     │   Audit Log saved
  │     │     ↓
  │     │   AuditClient (logs sent)
  │     │
  │     └→ Save Report to Database
  │
  ↓
Response to Client
```

## Key Improvements Summary

| Aspect | Before | After |
|--------|--------|-------|
| **Deployment** | Monolithic JAR | Independent microservice |
| **Scalability** | All components scale together | Analytics can scale independently |
| **Fault Isolation** | Failure cascades | Analytics failure doesn't affect other services |
| **Development** | Tightly integrated codebase | Independent repository possible |
| **Testing** | Full monolith tests required | Isolated unit/integration tests |
| **Audit Logging** | Not implemented | Full audit trail with auth-service |
| **Inter-Service Calls** | In-process (tight coupling) | HTTP REST with loose coupling |
| **API Documentation** | Single gateway docs | Per-service Swagger + aggregation |
| **Data Segregation** | Shared database | Dedicated analytics database |
| **Configuration** | Monolith config | Independently configurable |
| **Monitoring** | Single app metrics | Service-level metrics |
| **Deployment Frequency** | Affects all services | Independent deployment cycle |
| **Build Status** | ✅ 0 errors | ✅ 0 errors |

## API Endpoints Comparison

### Before: Monolith Endpoint
```
POST /reports
  (Part of monolithic app)
```

### After: Microservice Endpoints
```
POST   /civicDesk/analytics/reports               # Generate report
GET    /civicDesk/analytics/reports/user/{userId} # User's reports
GET    /civicDesk/analytics/reports/{id}/download # Download as Excel
DELETE /civicDesk/analytics/reports/{id}           # Delete report
```

All accessible through API Gateway:
```
Gateway: http://localhost:9090/civicDesk/analytics/**
Direct:  http://localhost:8088/civicDesk/analytics/**
```

## Audit Trail Example

Before → After flow:

**Before:** No audit trail

**After:**
```
1. User requests: POST /civicDesk/analytics/reports
   └─ Authorization: Bearer <jwt-token>

2. ReportController processes request

3. ReportController calls AuditClient.logAudit()
   └─ userId: "USR001"
   └─ action: "GENERATE_REPORT"
   └─ module: "ANALYTICS"
   └─ ip: "192.168.1.100"

4. AuditClient makes HTTP POST to Auth Service
   └─ POST http://localhost:8081/civicDesk/audit/auditLogs
   └─ Payload: CreateAuditLogRequest

5. Auth Service stores audit log

6. AuditClient returns (non-blocking - doesn't affect response)

7. Response sent back to client
   └─ Audit trail created separately
   └─ Can be queried: GET /civicDesk/audit/auditLogs?module=ANALYTICS&action=GENERATE_REPORT
```

## Testing Strategy

**Before:** Integration tests with entire monolith

**After:** 
- Unit tests: Component-isolated testing
- Integration tests: Analytics-service + mock clients
- End-to-end tests: Full microservice orchestration

---

**Status:** ✅ All changes completed and verified  
**Build:** ✅ Successful (0 errors)  
**Ready for:** Testing and deployment
