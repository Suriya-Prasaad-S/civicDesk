# CivicDesk API Gateway Architecture

## Overview

The API Gateway (port 9090) is the single entry point for all client requests to the CivicDesk microservices ecosystem. It enforces security, distributes traffic, handles resilience, and provides observability across the system.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                  CLIENTS                                    │
│        (Web Browser, Mobile App, External API, Admin Tools)                 │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │ HTTP/HTTPS Request
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         API GATEWAY (Port 9090)                             │
│                   Spring Cloud Gateway + Reactive Stack                     │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ 1. JWT Authentication Filter                                        │  │
│  │    - Extract Bearer token from Authorization header                 │  │
│  │    - Validate JWT signature (HS384 with shared secret)              │  │
│  │    - Return 401 if invalid                                          │  │
│  │    - Extract userId, role, correlationId for downstream            │  │
│  └──────────────────────────────┬───────────────────────────────────────┘  │
│                                 │                                          │
│  ┌──────────────────────────────▼───────────────────────────────────────┐  │
│  │ 2. Rate Limiting Filter (Global)                                    │  │
│  │    - Check request count against configured limits                  │  │
│  │    - MAX_REQUESTS_PER_MINUTE: 600 (configurable)                    │  │
│  │    - Return 429 Too Many Requests if exceeded                       │  │
│  │    - Redis-backed for distributed environments                      │  │
│  └──────────────────────────────┬───────────────────────────────────────┘  │
│                                 │                                          │
│  ┌──────────────────────────────▼───────────────────────────────────────┐  │
│  │ 3. Route Matching & Path-Based Routing                              │  │
│  │    - Match request path to defined routes                           │  │
│  │    - Routes configured in application.yml                           │  │
│  │    - 8 microservices routed by path predicates                      │  │
│  │    - Apply path rewrite filters if needed                           │  │
│  └──────────────────────────────┬───────────────────────────────────────┘  │
│                                 │                                          │
│  ┌──────────────────────────────▼───────────────────────────────────────┐  │
│  │ 4. Circuit Breaker (Resilience4j)                                   │  │
│  │    - Track success/failure rates for each downstream service        │  │
│  │    - CLOSED state: requests pass through normally                   │  │
│  │    - OPEN state: fail fast, return 503 without calling service      │  │
│  │    - HALF_OPEN: try a few requests to detect recovery               │  │
│  │    - Prevents cascading failures                                    │  │
│  └──────────────────────────────┬───────────────────────────────────────┘  │
│                                 │                                          │
│  ┌──────────────────────────────▼───────────────────────────────────────┐  │
│  │ 5. Distributed Tracing & Logging Filter                             │  │
│  │    - Generate/propagate X-Trace-ID and X-Correlation-ID             │  │
│  │    - Attach trace context to ThreadLocal / Reactor context          │  │
│  │    - Log request entry with userId, path, method                    │  │
│  │    - Send spans to Jaeger/OpenTelemetry backend                     │  │
│  └──────────────────────────────┬───────────────────────────────────────┘  │
│                                 │                                          │
│  ┌──────────────────────────────▼───────────────────────────────────────┐  │
│  │ 6. Forward Request to Downstream Service                            │  │
│  │    - Add internal headers (userId, role, traceId, correlationId)    │  │
│  │    - Use timeout policy (default 5s)                                │  │
│  │    - Capture response time for metrics                              │  │
│  └──────────────────────────────┬───────────────────────────────────────┘  │
│                                 │                                          │
│  ┌──────────────────────────────▼───────────────────────────────────────┐  │
│  │ 7. Metrics & Response Handling                                      │  │
│  │    - Record latency, status codes, error rates                      │  │
│  │    - Export to Prometheus at /actuator/prometheus                   │  │
│  │    - Log response status and payload size                           │  │
│  │    - Return response to client                                      │  │
│  └──────────────────────────────┬───────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────┬─────────────────────────────────────────┘
                                  │ Response
                                  ▼
                        ┌──────────────────────┐
                        │  CLIENT (with JWT)   │
                        └──────────────────────┘
```

---

## Microservices Overview

All 8 microservices communicate through the API Gateway and implement their own JWT validation and business logic:

### Service Directory

| Service | Port | Key Responsibilities | Controllers | Database |
|---------|------|----------------------|-------------|----------|
| **Auth Service** | 8081 | User authentication, JWT generation, role management, audit logging | AuthController, UserController, DepartmentController, AuditLogController | PostgreSQL (auth_db) |
| **Citizen Service** | 8082 | Citizen profiles, document management, citizen data | CitizenProfileController, CitizenDocumentController | PostgreSQL (citizen_db) |
| **Service Request Service** | 8083 | Service catalog, service requests, document uploads | ServiceRequestController, ServiceCatalogController, RequestDocumentController | PostgreSQL (service_request_db) |
| **Permit Service** | 8084 | Permit/license applications, inspections, approval workflows | PermitController, InspectionController | PostgreSQL (permit_db) |
| **Grievance Service** | 8085 | Grievance filing, escalation, multi-role (citizen/officer/supervisor) handling | CitizenGrievanceController, FieldOfficerGrievanceController, SupervisorGrievanceController | PostgreSQL (grievance_db) |
| **Public Works Service** | 8086 | Work orders, milestones, project tracking, budget management | WorkOrderController, MilestoneController | PostgreSQL (public_works_db) |
| **Notification Service** | 8087 | Email, SMS, in-app notifications, notification templates | NotificationController | PostgreSQL (notification_db) |
| **Analytics Service** | 8088 | Reports, dashboards, metrics, data aggregation | ReportController | PostgreSQL (analytics_db) |

### Service Implementation Details

#### 1. Auth Service (Port 8081)
**Endpoints**:
- `POST /iam/auth/register` — Citizen/Staff registration
- `POST /iam/auth/citizen/login` — Citizen login with email/password
- `POST /iam/auth/staff/login` — Staff login with email/password
- `POST /iam/auth/refresh-token` — Refresh JWT token
- `POST /iam/auth/validate-token` — Validate JWT token
- `POST /iam/auth/revoke-token` — Logout (blacklist token)
- `POST /iam/auth/setPassword` — Set/reset password
- `GET /iam/users/{id}` — Fetch user details
- `POST /iam/departments` — Manage departments
- `GET /iam/audit-logs` — View audit logs

**Key Features**:
- ✅ JWT (HS384) token generation and validation
- ✅ Role-based authentication (CITIZEN, STAFF, ADMIN)
- ✅ Password encryption (bcrypt)
- ✅ Revoked token repository (Redis-compatible)
- ✅ Audit logging (all auth events with IP tracking)
- ✅ Department/role management

---

#### 2. Citizen Service (Port 8082)
**Endpoints**:
- `POST /civicDesk/citizens/register` — Register new citizen
- `GET /civicDesk/citizens/profile` — Fetch citizen profile
- `PUT /civicDesk/citizens/profile` — Update citizen profile
- `GET /civicDesk/citizens/{id}` — Get citizen by ID
- `POST /civicDesk/citizen-documents/upload` — Upload citizen documents
- `GET /civicDesk/citizen-documents/{id}` — Retrieve documents

**Key Features**:
- ✅ Citizen profile management
- ✅ Document upload/download (file storage)
- ✅ Citizen search and filtering
- ✅ Profile validation
- ✅ Data export capabilities

---

#### 3. Service Request Service (Port 8083)
**Endpoints**:
- `GET /civicDesk/serviceRequest/getAllServices` — List all services in catalog
- `GET /civicDesk/serviceRequest/getService/{id}` — Get service details
- `POST /civicDesk/serviceRequest/submit` — Submit service request
- `GET /civicDesk/serviceRequest/{id}` — Get request status
- `PUT /civicDesk/serviceRequest/{id}` — Update request
- `POST /civicDesk/service-documents/upload` — Upload documents for request

**Key Features**:
- ✅ Service catalog with descriptions and fees
- ✅ Request lifecycle management (SUBMITTED → IN_PROGRESS → COMPLETED)
- ✅ Document attachment support
- ✅ Status tracking and notifications
- ✅ Service feedback/rating

---

#### 4. Permit Service (Port 8084)
**Endpoints**:
- `POST /civicDesk/permits/apply` — Apply for permit/license
- `GET /civicDesk/permits/{id}` — Get permit details
- `PUT /civicDesk/permits/{id}/approve` — Approve permit (admin)
- `POST /civicDesk/inspections/schedule` — Schedule inspection
- `GET /civicDesk/inspections/{id}` — Get inspection details
- `PUT /civicDesk/inspections/{id}/complete` — Complete inspection

**Key Features**:
- ✅ Permit application workflows
- ✅ Multi-step approval process
- ✅ Inspection scheduling and tracking
- ✅ Compliance documentation
- ✅ Fee calculation and payment tracking
- ✅ Renewal reminders

---

#### 5. Grievance Service (Port 8085)
**Endpoints**:
- `POST /civicDesk/grievance/file` — File new grievance
- `GET /civicDesk/grievance/{id}` — Get grievance details
- `PUT /civicDesk/grievance/{id}/escalate` — Escalate grievance (citizen)
- `PUT /civicDesk/grievance/{id}/assign` — Assign to officer (supervisor)
- `PUT /civicDesk/grievance/{id}/update-status` — Update status (officer/supervisor)
- `GET /civicDesk/grievance/my-grievances` — List user's grievances

**Key Features**:
- ✅ Multi-role support (Citizen, Field Officer, Supervisor)
- ✅ Grievance categorization
- ✅ Escalation workflow
- ✅ Assignment tracking
- ✅ SLA monitoring (time-to-resolve)
- ✅ Resolution documentation
- ✅ Feedback collection

---

#### 6. Public Works Service (Port 8086)
**Endpoints**:
- `POST /civicDesk/workorders/create` — Create work order
- `GET /civicDesk/workorders/{id}` — Get work order details
- `GET /civicDesk/workorders/public/{id}` — Public view (citizen can see)
- `PUT /civicDesk/workorders/{id}/status` — Update status
- `POST /civicDesk/milestones/create` — Create project milestone
- `GET /civicDesk/milestones/{id}` — Get milestone details

**Key Features**:
- ✅ Work order lifecycle management
- ✅ Milestone tracking and progress
- ✅ Budget allocation and tracking
- ✅ Resource assignment
- ✅ Public visibility for citizen awareness
- ✅ Completion documentation

---

#### 7. Notification Service (Port 8087)
**Endpoints**:
- `POST /civicDesk/notification/send` — Send notification
- `GET /civicDesk/notification/templates` — List message templates
- `POST /civicDesk/notification/subscribe` — Subscribe to notification channel
- `GET /civicDesk/notification/history` — View notification history

**Key Features**:
- ✅ Multi-channel notifications (Email, SMS, In-app)
- ✅ Template-based messaging
- ✅ Notification scheduling
- ✅ User preference management
- ✅ Retry logic for failed sends (async)
- ✅ External service integration (SMTP, Twilio/AWS SNS)

---

#### 8. Analytics Service (Port 8088)
**Endpoints**:
- `GET /civicDesk/analytics/reports` — List available reports
- `GET /civicDesk/analytics/report/{id}` — Generate specific report
- `GET /civicDesk/analytics/dashboards` — Get dashboard data
- `GET /civicDesk/analytics/export/{id}` — Export report (CSV/PDF)

**Key Features**:
- ✅ Real-time dashboards
- ✅ Historical data aggregation
- ✅ Service metrics and KPIs
- ✅ Citizen satisfaction metrics
- ✅ Department performance reports
- ✅ Export to multiple formats (CSV, PDF, Excel)
- ✅ Scheduled report generation

---

## Component Details

### 1. Authentication & Authorization

#### How It Works

- **Token Generation**: User logs in via `POST /iam/auth/citizen/login` or `/staff/login` → Auth Service generates HS384-signed JWT
- **Token Structure**:
  ```json
  {
    "userId": "1000001",
    "role": "ADMIN",
    "iat": 1783931075,
    "exp": 1783932875
  }
  ```

#### Gateway-Level Authentication

**File**: `api-gateway/src/main/java/com/civicdesk/gateway/filter/JwtAuthenticationFilter.java`

```java
if (!jwtTokenProvider.validateToken(token)) {
    return unauthorized(exchange);  // 401 Unauthorized
}

// Extract claims for downstream
Claims claims = jwtTokenProvider.getClaims(token);
String userId = claims.getSubject();
String role = jwtTokenProvider.getRoleFromToken(token);

// Pass to downstream service
exchange.getAttributes().put("userId", userId);
exchange.getAttributes().put("role", role);
```

#### Key Features

- **Shared Secret**: All services use same `app.jwt.secret` for validation
- **Public Paths**: `/registra`, `/login`, `/swagger-ui/**`, `/actuator/health` bypass auth
- **Token Validation**:
  - Check JWT signature (HS384)
  - Check expiration
  - Check if token is revoked (in `RevokedTokenRepository`)
- **Role-Based Access**: Authorization at service level (each service checks role in SecurityContextUtil)

#### Endpoint Usage by Services

```
┌─────────────────┬──────────────────┬──────────────────────────────┐
│ Service         │ Endpoint         │ Purpose                      │
├─────────────────┼──────────────────┼──────────────────────────────┤
│ Gateway         │ /validate-token  │ Pre-request auth check       │
│ Services        │ Local decode     │ Extract userId/role          │
│ Client          │ /refresh-token   │ Get new token on expiry      │
│ Client/Admin    │ /revoke-token    │ Logout: blacklist token      │
└─────────────────┴──────────────────┴──────────────────────────────┘
```

---

### 2. Request Routing

#### Configuration

**File**: `api-gateway/src/main/resources/application.yml`

```yaml
spring:
  cloud:
    gateway:
      routes:
        # Auth Service (8081)
        - id: auth-service
          uri: http://localhost:8081
          predicates:
            - Path=/civicDesk/iam/**
          filters:
            - name: JwtAuthentication
            - name: RateLimiting

        # Citizen Service (8082)
        - id: citizen-service
          uri: http://localhost:8082
          predicates:
            - Path=/civicDesk/citizens/**

        # ... more routes for other services
```

#### Routing Logic

```
Request: GET /civicDesk/serviceRequest/getAllServices
         ↓
Gateway Pattern Matching:
  Check Path=/civicDesk/serviceRequest/** → Match
         ↓
Route Definition:
  id: service-request-service
  uri: http://localhost:8083
  predicates: Path=/civicDesk/serviceRequest/**
  filters: [JwtAuthentication, RateLimiting, CircuitBreaker]
         ↓
Forward to: http://localhost:8083/civicDesk/serviceRequest/getAllServices
         ↓
Service Response → Gateway → Client
```

#### 8 Microservices Routed

```
┌──────────────────────────────────────────────────────────────────┐
│                      API GATEWAY ROUTES                          │
├──────────────────────────┬────────────────┬─────────────────────┤
│ Service                  │ Port           │ Path Pattern        │
├──────────────────────────┼────────────────┼─────────────────────┤
│ Auth Service             │ 8081           │ /civicDesk/iam/**   │
│ Citizen Service          │ 8082           │ /civicDesk/citizen**│
│ Service Request Service  │ 8083           │ /civicDesk/service**│
│ Permit Service           │ 8084           │ /civicDesk/permit** │
│ Grievance Service        │ 8085           │ /civicDesk/grievan**│
│ Public Works Service     │ 8086           │ /civicDesk/workord**│
│ Notification Service     │8087           │ /civicDesk/notif**  │
│ Analytics Service        │ 8088           │ /civicDesk/analyt** │
└──────────────────────────┴────────────────┴─────────────────────┘
```

---

### 3. Rate Limiting

#### Implementation

**File**: `api-gateway/src/main/java/com/civicdesk/gateway/filter/RateLimitingFilter.java`

#### Current Behavior (In-Memory)

- **Global Counter**: Tracks total requests per minute
- **Limit**: 600 requests/minute (currently lowered to 5 for testing)
- **Reset**: Every 60 seconds
- **Response**: HTTP 429 with `Retry-After: 5` header

```java
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String clientId = extractClientIdentifier(exchange.getRequest());
    
    // Simple in-memory rate limiting
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastResetTime > 60000) {
        requestCount = 0;
        lastResetTime = currentTime;
    }
    
    if (requestCount >= MAX_REQUESTS_PER_MINUTE) {
        return rateLimitExceeded(exchange);  // 429
    }
    
    requestCount++;
    return chain.filter(exchange);
}
```

#### Alternative: Redis-Backed Distributed Rate Limiting

**File**: `api-gateway/src/main/java/com/civicdesk/gateway/service/RateLimitingService.java`

For distributed deployments, use `RateLimitingService` with Redis:

```java
public Mono<Boolean> isRequestAllowed(String clientId, String path) {
    String key = String.format("ratelimit:%s:%s:%d", clientId, path, currentSecond);
    
    // Check Redis key, increment, set TTL
    if (count < requestsPerSecond) {
        redisTemplate.opsForValue()
            .set(key, String.valueOf(count + 1), Duration.ofSeconds(2));
        return Mono.just(true);
    }
    return Mono.just(false);
}
```

**Benefits**: Per-client limits, shared across gateway instances via Redis

#### Configuration (application.yml)

```yaml
gateway:
  rate-limit:
    requests-per-minute: 600
    requests-per-second: 10

# Redis for distributed rate limiting
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
```

---

### 4. Circuit Breaker

#### Implementation

**Technology**: Resilience4j

**File**: `api-gateway/src/main/java/com/civicdesk/gateway/config/CircuitBreakerConfiguration.java`

**Configuration** (application.yml):

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        registerHealthIndicator: true
        slidingWindowSize: 100
        failureRateThreshold: 50
        slowCallRateThreshold: 100
        slowCallDurationThreshold: 2000
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 30000
        
    instances:
      auth-service:
        baseConfig: default
      citizen-service:
        baseConfig: default
      # ... more instances per service
```

#### State Machine

```
                              Failure Rate > 50%
                         or Slow Call Rate > 100%
                                   │
                                   ▼
┌──────────────┐           ┌──────────────┐
│   CLOSED     │──Request──│    OPEN      │
│              │   Pass    │              │
│   Normal     │           │   Fast Fail  │
│   Ops        │           │   (503)      │
└──────────────┘           └──────┬───────┘
      ▲                           │
      │                    Wait 30s
      │                           │
      └───Success in Sampling─────▼
          3/3 requests            │
                            ┌──────────────┐
                            │ HALF_OPEN    │
                            │              │
                            │ Try 3 Reqs   │
                            └──────────────┘
```

#### How It Works with Gateway

1. **Tracking**: Each downstream service has a circuit breaker instance
2. **On Request Failure**:
   - Count failures in sliding window (100 requests)
   - If failure rate > 50% → **OPEN** state
3. **In OPEN State**:
   - Reject all requests immediately → **503 Service Unavailable**
   - Prevents hammering failing service
4. **Recovery** (After 30s):
   - Transition to **HALF_OPEN**
   - Allow 3 sample requests
   - If successful → **CLOSED** (resume normal)
   - If fails → **OPEN** again (extend recovery)

#### Monitoring Circuit Breaker Status

```bash
# Check health of all circuit breakers
curl http://localhost:9090/actuator/health

# Detailed resilience4j metrics
curl http://localhost:9090/actuator/prometheus | grep circuitbreaker
```

---

### 5. Logging, Monitoring & Distributed Tracing

#### Architecture

```
┌──────────────┐
│ API Gateway  │
│              │
│ - Logs       │◄──┐
│ - Metrics    │   │
│ - Traces     │   │
└──────────────┘   │
       │           │
       │ Step 1: Generate/Add Trace Context
       │ X-Trace-ID: 550e8400-e29b-41d4-a716-446655440000
       │ X-Correlation-ID: req-12345
       │
       ▼           │
┌──────────────┐   │
│ Microservice │   │
│   (8083)     │   │
│              │   │
│ - Logs       │───┤
│ - Metrics    │◄──┘
│ - Traces     │
└──────────────┘
       │
       │ Step 2: Send spans to tracing backend
       │
       ▼
┌──────────────────────────────────────────┐
│  Observations Backend                    │
│  (Jaeger / OpenTelemetry Collector)      │
│  - Stores traces, spans, timing          │
│  - Visualizes request flow across all    │
│    services                              │
└──────────────────────────────────────────┘
       │
       ▼
┌──────────────────┐
│  Prometheus      │
│  - Metrics DB    │
│  - Scraped every │
│    15s from      │
│    /actuator/    │
│    prometheus    │
└──────────────────┘
       │
       ▼
┌──────────────────┐
│  Grafana         │
│  - Dashboards    │
│  - Alerts        │
│  - Real-time     │
│    visualization │
└──────────────────┘
```

#### 5a. Structured Logging

**File**: `api-gateway/src/main/resources/logback-spring.xml`

**Configuration**:

```xml
<appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
    <appender-ref ref="FILE" />
    <queueSize>512</queueSize>
    <discardingThreshold>0</discardingThreshold>
</appender>

<logger name="com.civicdesk.gateway" level="DEBUG" />
<logger name="org.springframework.cloud.gateway" level="INFO" />
<logger name="org.springframework.security" level="DEBUG" />
```

**Log Output** (includes trace ID):

```
2026-07-13 14:05:00 [main] INFO  c.c.gateway.ApiGatewayApplication - Starting ApiGatewayApplication
2026-07-13 14:05:02 [parallel-1] DEBUG c.c.gateway.filter.JwtAuthenticationFilter - userId=1000001 role=ADMIN traceId=550e8400-e29b-41d4 path=/civicDesk/citizens/list
2026-07-13 14:05:02 [parallel-2] DEBUG c.c.gateway.filter.RateLimitingFilter - requestCount=5 limit=600 allowed=true
2026-07-13 14:05:03 [parallel-3] INFO  c.c.gateway.filter.TracingFilter - Response: 200 OK latency=1234ms traceId=550e8400-e29b-41d4
```

#### 5b. Metrics Export (Prometheus)

**Configuration** (application.yml):

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics,circuitbreakers,circuitbreaker-events
      base-path: /actuator
  
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      slo:
        http.server.requests: 50ms,100ms,200ms,500ms,1s,2s,5s
```

**Scraped Metrics**:

```
# At /actuator/prometheus (scraped by Prometheus every 15s)
http_server_requests_seconds_bucket{method="POST",path="/civicDesk/citizens/register",status="201",le="0.05"} 120
http_server_requests_seconds_bucket{method="GET",path="/civicDesk/serviceRequest/getAllServices",status="200",le="1.0"} 456
http_server_requests_seconds_sum{method="GET",status="429"} 3.50  # Rate limit
circuitbreaker_calls_total{name="auth-service",state="CLOSED"} 5000
circuitbreaker_calls_total{name="auth-service",state="OPEN"} 32
```

**Dashboard Queries** (Grafana):

```
# Request latency (p95)
histogram_quantile(0.95, http_server_requests_seconds_bucket)

# Error rate %
rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m]) * 100

# Circuit breaker status
circuitbreaker_calls_total{state="OPEN"}
```

#### 5c. Distributed Tracing (OpenTelemetry + Jaeger)

**Configuration** (application.yml):

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # Sample 100% in dev; reduce in prod
    propagation:
      type: jaeger
```

**How It Works**:

1. **Gateway Generates Trace**:
   ```java
   String traceId = UUID.randomUUID().toString();
   exchange.getAttributes().put("traceId", traceId);
   ```

2. **Trace Context Propagation** (filters):
   ```
   Request Header: X-Trace-ID: 550e8400-e29b-41d4-a716-446655440000
   Request Header: X-Correlation-ID: req-12345-user-1000001
   ```

3. **Each Service Receives & Forwards**:
   ```java
   // In downstream service filter:
   String traceId = request.getHeader("X-Trace-ID");
   MDC.put("traceId", traceId);  // Adds to all logs
   
   // If service calls another:
   outgoingRequest.setHeader("X-Trace-ID", traceId);
   ```

4. **Span Recording**:
   ```
   Span 1 (Gateway):
     - operationName: POST /civicDesk/citizens/register
     - duration: 234ms
     - tags: userId=1000001, statusCode=201
   
   Span 2 (Auth Service):
     - operationName: validateCredentials
     - duration: 45ms
     - tags: email=user@example.com, result=success
   
   Span 3 (Citizen Service):
     - operationName: registerCitizen
     - duration: 150ms
     - tags: citizenId=2000001
   ```

5. **Jaeger UI Visualization**:
   ```
   Timeline View:
   ─ Request Entry (0ms)
     ├─ JwtAuthentication (5ms)
     ├─ RateLimitingFilter (2ms)
     ├─ ForwardToCitizen (234ms)
     │  └─ CitizenService processing (228ms)
     └─ Response (1ms)
   
   Total Latency: 242ms
   ```

#### 5d. Health Checks

**Endpoint**: `GET /actuator/health`

```json
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "auth-service": "CLOSED",
        "citizen-service": "CLOSED",
        "service-request-service": "OPEN"  // ← Failing, open CB
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": { "total": 500107862016, "free": 400000000000 }
    },
    "livenessState": { "status": "UP" },
    "readinessState": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}
```

---

## Complete Request Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ CLIENT SENDS REQUEST                                                         │
│ POST /civicDesk/citizens/register                                           │
│ Authorization: Bearer eyJhbGci...                                           │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ [1] API GATEWAY - Request Received (9090)                                   │
│                                                                             │
│ ✓ JwtAuthenticationFilter                                                   │
│   - Extract: Bearer token from header                                       │
│   - Validate: JWT signature (HS384 + shared secret)                         │
│   - Extract: userId=1000001, role=CITIZEN                                   │
│   - Status: ✓ Valid → Continue                                              │
│                                                                             │
│ ✓ RateLimitingFilter                                                        │
│   - Check: requestCount (5) < MAX (600)                                     │
│   - Status: ✓ Allowed → requestCount++                                      │
│                                                                             │
│ ✓ Route Matching                                                            │
│   - Pattern: Path=/civicDesk/citizens/**                                    │
│   - Matched Route: citizen-service                                          │
│   - Target: http://localhost:8082                                           │
│                                                                             │
│ ✓ Tracing Filter                                                            │
│   - Generate: X-Trace-ID=550e8400-e29b-41d4                                │
│   - Generate: X-Correlation-ID=req-12345-user-1000001                      │
│   - Add to MDC for logging                                                  │
│                                                                             │
│ ✓ Circuit Breaker Check                                                     │
│   - citizen-service state: CLOSED (operational)                             │
│   - Status: ✓ Can forward request                                           │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                    Add Internal Headers:
                    X-User-ID: 1000001
                    X-Role: CITIZEN
                    X-Trace-ID: 550e8400...
                    X-Correlation-ID: req-12345...
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ [2] CITIZEN SERVICE - Request Received (8082)                               │
│                                                                             │
│ ✓ JwtAuthFilter (local validation)                                          │
│   - Extract X-User-ID header: 1000001                                       │
│   - Validate JWT with local copy of secret                                  │
│   - Set SecurityContext: userId=1000001, role=CITIZEN                       │
│                                                                             │
│ ✓ Tracing MDC (inherit from gateway)                                        │
│   - X-Trace-ID from request header                                          │
│   - Log all operations with this traceId                                    │
│                                                                             │
│ ✓ Business Logic                                                            │
│   - Register citizen: INSERT INTO citizen ...                               │
│   - Response: { citizenId: 2000001, status: "created" }                     │
│                                                                             │
│ ✓ Audit Logging                                                             │
│   - Log: action=REGISTER, userId=1000001, timestamp=..., traceId=...       │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                    Response: 201 Created
                    {
                      "citizenId": 2000001,
                      "status": "created"
                    }
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ [3] API GATEWAY - Response Handling                                         │
│                                                                             │
│ ✓ Metrics Recording                                                         │
│   - status: 201                                                             │
│   - method: POST                                                            │
│   - path: /civicDesk/citizens/register                                      │
│   - duration_ms: 234                                                        │
│   - Export to Prometheus                                                    │
│                                                                             │
│ ✓ Circuit Breaker Update                                                    │
│   - citizen-service: success → decrement failure count                       │
│   - Keep state: CLOSED                                                      │
│                                                                             │
│ ✓ Tracing Completion                                                        │
│   - Span: POST /civicDesk/citizens/register (234ms)                         │
│   - Status: 201                                                             │
│   - Send to Jaeger backend                                                  │
│                                                                             │
│ ✓ Response to Client                                                        │
│   - Status: 201 Created                                                     │
│   - Body: { citizenId: 2000001, ... }                                       │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ CLIENT RECEIVES RESPONSE                                                     │
│ Status: 201 Created                                                         │
│ { "citizenId": 2000001, "status": "created" }                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Failure Scenarios & Resilience

### Scenario 1: Invalid JWT Token

```
Request: GET /civicDesk/citizens/1000001
Header: Authorization: Bearer invalid_token_xyz

Gateway JwtAuthenticationFilter:
├─ jwtTokenProvider.validateToken(invalid_token_xyz)
└─ ✗ Signature verification fails
   └─ return 401 Unauthorized

Response: 401 Unauthorized
Body: { "error": "Invalid token" }

(Request never reaches downstream service)
```

### Scenario 2: Rate Limit Exceeded

```
Request 601: GET /civicDesk/serviceRequest/getAllServices
(600 requests already processed in this minute)

Gateway RateLimitingFilter:
├─ requestCount=600 >= MAX_REQUESTS_PER_MINUTE (600)
└─ ✗ Limit exceeded
   └─ return rateLimitExceeded(exchange)

Response: 429 Too Many Requests
Header: Retry-After: 60
Body: { "error": "Rate limit exceeded" }

(Request never reaches downstream service)
```

### Scenario 3: Downstream Service Failing (Circuit Breaker Protection)

```
Scenario: citizen-service crashes, 10 consecutive failures

Request N: POST /civicDesk/citizens/update
Gateway CircuitBreaker (citizen-service):
├─ Sliding window: failures=10/100
├─ failure rate: 10% < 50% threshold
├─ State: CLOSED → request proceeds

Request N+50: POST /civicDesk/citizens/update
Gateway CircuitBreaker (citizen-service):
├─ Sliding window: failures=60/100 (still failing)
├─ failure rate: 60% > 50% threshold
├─ State: CLOSED → OPEN (trip breaker)
│  └─ Fast-fail all requests for 30s

Request N+51 (while OPEN):
└─ return 503 Service Unavailable (without calling service)

After 30s (HALF_OPEN retry):
├─ Request N+200: Try sample request
├─ ✓ citizen-service responds successfully
├─ State: HALF_OPEN → CLOSED
└─ Resume normal operations
```

### Scenario 4: Revoked Token (Logout)

```
User Action: Client calls POST /iam/auth/revoke-token
├─ Token added to RevokedTokenRepository
└─ Other instances notified (if distributed)

Next Request: GET /civicDesk/citizens/profile
Header: Authorization: Bearer <same_revoked_token>

Auth Service validateToken():
├─ Check Redis/DB: existsById(token) = true (revoked)
├─ Return false
└─ Reject request

Response: 401 Unauthorized
Body: { "error": "Token invalid or expired" }
```

---

---

## Service-to-Service Integration Matrix

### How Microservices Interact

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        MICROSERVICES INTERACTION MAP                     │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Citizen Service ─────────┐                                             │
│        (8082)             │                                             │
│        ↓                  │                                             │
│  [Calls Auth Service]     │  [Validates JWT locally]                   │
│        ↓                  │                                             │
│  Auth Service ────────────┼──→ Role/Permission Check                   │
│     (8081)                │     [Shared Secret]                         │
│                           │                                             │
│       ↓                   ↓                                             │
│   Audit Log ←─ All Service Calls (for compliance)                      │
│                                                                          │
│  Service Request Service ─────┐                                         │
│        (8083)                 │                                         │
│        ↓                      │                                         │
│  [Trigger Notifications] ─→ Notification Service (8087)                │
│                              [Async event-based]                        │
│                                                                          │
│  Permit Service ──────────┐                                             │
│        (8084)             │                                             │
│        ↓                  │                                             │
│  [Trigger Notifications] ─→ Notification Service (8087)                │
│                              [Inspection scheduled, approved]           │
│                                                                          │
│  Grievance Service ───────┐                                             │
│        (8085)             │                                             │
│        ↓                  │                                             │
│  [Trigger Notifications] ─→ Notification Service (8087)                │
│  [Escalations]               [Filed, escalated, resolved]              │
│                                                                          │
│  Public Works Service ────┐                                             │
│        (8086)             │                                             │
│        ↓                  │                                             │
│  [Trigger Notifications] ─→ Notification Service (8087)                │
│  [Status Updates]           [Work started, completed]                   │
│                                                                          │
│  Analytics Service ───────┐                                             │
│        (8088)             │                                             │
│        ↓                  │                                             │
│  [Aggregate Data] ────→ All Services (query completion)                │
│                         [Dashboard generation]                          │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### Communication Patterns

#### Pattern 1: Direct Service-to-Service Calls (Synchronous)
```
Citizen Service needs to verify user role before processing:

CitizenService.java:
  ├─ receiveRequest(userId)
  ├─ Call: authServiceClient.getUserRole(userId)
  │   │
  │   └─→ HTTP Call to Auth Service (8081)
  │       Auth Service responds with role
  │
  ├─ Check role → Allow/Deny
  └─ Return response
```

#### Pattern 2: Event-Based Notifications (Asynchronous)
```
Permit Service approves a permit → need to notify citizen:

PermitService.java:
  ├─ receiveApprovalRequest(permitId)
  ├─ Approve permit (DB update)
  ├─ Publish Event: "PERMIT_APPROVED" 
  │
  └─→ (async) Notification Service consumes event
      NotificationService.java:
        ├─ Listen to "PERMIT_APPROVED" events
        ├─ Get citizen email
        ├─ Send notification (email/SMS)
        └─ Log delivery status
```

---

## Service-Level Security & Validation

### Every Service Implements:

```
1. JwtAuthFilter
   ├─ Extract token from request header
   ├─ Validate JWT signature (local copy of secret)
   ├─ Check expiration
   ├─ Return 401 if invalid

2. Local JWT Validation (Redundant Security)
   ├─ Decode JWT claims
   ├─ Extract userId, role
   ├─ Set SecurityContext
   └─ All methods have role checks via @Secured annotations

3. Audit Logging
   ├─ Log all requests (userId, action, timestamp, status)
   ├─ Include trace ID for correlation
   ├─ Send to centralized log aggregator

4. Error Handling
   ├─ 401 Unauthorized (invalid JWT)
   ├─ 403 Forbidden (insufficient role)
   ├─ 404 Not Found (resource not found)
   ├─ 500 Internal Server Error (with correlation ID)
```

### Example: Citizen Service Request Processing

```
1. Client Request arrives at Gateway (Port 9090)
   GET /civicDesk/citizens/profile
   Header: Authorization: Bearer <JWT>

2. Gateway JwtAuthenticationFilter
   ├─ Extract token
   ├─ Validate signature (HS384 + shared secret)
   ├─ Extract: userId=1000001, role=CITIZEN
   └─ ✓ Pass to Citizen Service

3. Citizen Service (Port 8082) receives request
   JwtAuthFilter (local):
   ├─ Re-validate JWT (local secret)
   ├─ Check not in revoke list (Redis)
   ├─ Extract userId from JWT
   └─ Set SecurityContext

4. CitizenProfileController
   @Secured({"ROLE_CITIZEN", "ROLE_ADMIN"})
   public ResponseEntity getProfile(userId) {
       ├─ Query database
       ├─ Log: userId=1000001, action=VIEW_PROFILE
       └─ Return profile data
   }

5. Response back to Gateway
   ├─ Record in Prometheus: latency=45ms, status=200
   ├─ Send span to Jaeger: "GET /civicDesk/citizens/profile"
   ├─ Add trace ID to all logs

6. Response to Client
   Status: 200 OK
   Body: {citizen profile data}
```

---

## Data Consistency & Eventual Consistency Patterns

### Strongly Consistent Operations
```
Scenarios requiring immediate consistency:

1. Login (Authentication)
   - User provides credentials
   - Auth Service validates immediately
   - Returns token or 401

2. Permit Approval (Authorization check)
   - Officer approves permit
   - Citizen immediately sees "APPROVED" status
   - No eventual consistency needed
```

### Eventually Consistent Operations
```
Scenarios using async patterns:

1. Notification Delivery (Async)
   Permit approved → trigger notification
   ├─ Permit Service: marks permit as APPROVED (commit)
   ├─ Async: Notification Service eventually sends email
   ├─ If email fails: Retry mechanism kicks in
   └─ Notification Service logs delivery status

2. Analytics (Batch Processing)
   └─ Analytics Service periodically aggregates data
      from all services (eventual consistency acceptable)

3. Audit Log Aggregation
   └─ Services publish audit events
      Centralized logging system eventually ingests
      (milliseconds to seconds delay acceptable)
```

---

## Deployment View (All Services)

```
                          ┌─ Load Balancer ◄──── HTTPS (443)
                          │
       ┌──────────────────┴──────────────────┐
       │                                     │
       ▼                                     ▼
┌────────────────┐                  ┌────────────────┐
│ API Gateway 1  │                  │ API Gateway 2  │
│ Port 9090      │──────┬───────────│ Port 9090      │
└────┬───────────┘      │           └────────┬───────┘
     │                  │                    │
     └──────────────────┼────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │ Shared Infrastructure
        ▼               ▼               ▼
    ┌────────┐    ┌─────────┐   ┌───────────┐
    │ Redis  │    │Postgres │   │Jaeger     │
    │        │    │ Cluster │   │Prometheus │
    └────────┘    └─────────┘   │ELK Stack  │
                                 └───────────┘

Microservices Layer (Behind Internal Network)
        ↓
   ┌────────────────────────────────────────┐
   │ 8 Microservices (each in container)    │
   ├────────────────────────────────────────┤
   │ ├─ Auth Service (8081)                 │
   │ ├─ Citizen Service (8082)              │
   │ ├─ Service Request (8083)              │
   │ ├─ Permit Service (8084)               │
   │ ├─ Grievance Service (8085)            │
   │ ├─ Public Works Service (8086)         │
   │ ├─ Notification Service (8087)         │
   │ └─ Analytics Service (8088)            │
   └────────────────────────────────────────┘
                    ↓
        ┌───────────┴───────────┐
        ▼                       ▼
   ┌─────────┐           ┌───────────┐
   │PostgreSQL│          │External    │
   │Multi-DB  │          │Services    │
   │(8 per    │          │(Email,SMS) │
   │service)  │          │            │
   └─────────┘           └───────────┘
```

---

## Service-to-Observability Integration

### Each Service Exports:

```
Prometheus Metrics (scrape every 15s):
  ├─ http_server_requests_seconds_* (latency histograms)
  ├─ http_server_requests_count (request counts by status)
  ├─ database_query_duration_seconds (DB query performance)
  ├─ cache_hits_total (Redis cache stats if used)
  └─ Custom business metrics (permits_created, grievances_filed, etc.)

Jaeger Tracing Spans:
  ├─ Operation name: "GET /civicDesk/citizens/profile"
  ├─ Duration: actual processing time
  ├─ Status: success/failure
  ├─ Tags: userId, role, resource_id, database_calls
  └─ Logs: significant events within the operation

Structured Logs:
  ├─ Timestamp
  ├─ Trace ID (propagated from gateway)
  ├─ Correlation ID (user journey tracking)
  ├─ Service name
  ├─ Method/Endpoint
  ├─ UserId
  ├─ Status code
  ├─ Duration
  └─ Error details (if applicable)

Example Log Line:
  2026-07-13 14:05:02.123 [citizen-service-pool-1] INFO  
  Com.civicdesk.citizen.controller.CitizenProfileController - 
  traceId=550e8400 correlationId=req-12345-user-1000001 
  userId=1000001 method=GET endpoint=/civicDesk/citizens/profile 
  status=200 duration=45ms
```

---

## Service-Level Resilience Features

Each service implements:

```
1. Timeout Policies
   ├─ Database queries: 5s timeout
   ├─ External API calls: 10s timeout
   ├─ Internal service calls: 5s timeout
   └─ Return graceful errors on timeout

2. Retry Logic
   ├─ Notification Service: Retry failed sends 3x
   ├─ Database: Retry transient errors (deadlock) 2x
   ├─ External calls: Exponential backoff

3. Circuit Breaker per External Call
   ├─ If Auth Service down → Fail fast (503)
   ├─ If Database down → Fail fast with error
   └─ Auto-recover after 30s

4. Graceful Degradation
   ├─ If Analytics unavailable → Return stale data
   ├─ If Notification fails → Log and continue
   └─ Non-critical failures don't block requests
```

---

## Service-to-Service Communication

### Direct Calls (Synchronous)

```
Example: Grievance Service needs officer details from Auth Service

GrievanceService.java:
    @Override
    public void assignGrievance(String grievanceId, String officerId) {
        // 1. Get officer details from Auth Service
        User officer = authServiceClient
            .getUser(officerId, getJwtToken())  // Pass JWT token
            .block(Duration.ofSeconds(5));       // 5s timeout
        
        if (officer == null) {
            throw new ExternalServiceException("Cannot reach Auth Service");
        }
        
        // 2. Validate officer is active
        if (!officer.getStatus().equals("ACTIVE")) {
            throw new BusinessRuleException("Officer is not active");
        }
        
        // 3. Update grievance with assignment
        grievanceRepository.updateAssignment(grievanceId, officerId);
        
        // 4. Trigger async event for notification
        grievanceEventPublisher.publish(
            new GrievanceAssignedEvent(grievanceId, officer.getEmail())
        );
    }
```

### Async Events (Asynchronous)

```
Example: Service Request filed → Notification sent

ServiceRequestService.java:
    public ServiceRequest submitRequest(ServiceRequest request) {
        // 1. Persist to database
        ServiceRequest saved = serviceRequestRepository.save(request);
        
        // 2. Publish event (non-blocking)
        applicationEvents.publishEvent(
            new ServiceRequestSubmittedEvent(
                saved.getId(),
                saved.getCitizenId(),
                saved.getServiceType()
            )
        );
        
        // 3. Return immediately (don't wait for notification)
        return saved;
    }

NotificationService (Event Listener):
    @EventListener
    public void onServiceRequestSubmitted(ServiceRequestSubmittedEvent event) {
        try {
            // 1. Get citizen email
            Citizen citizen = citizenServiceClient.getCitizen(event.getCitizenId());
            
            // 2. Send email
            emailService.send(
                citizen.getEmail(),
                "Service Request Submitted",
                "Your request has been received. ID: " + event.getRequestId()
            );
            
            // 3. Log success
            notificationRepository.logSuccess(event.getRequestId());
        } catch (Exception e) {
            // 4. Retry mechanism handles this
            logger.error("Failed to notify citizen", e);
            retryService.scheduleRetry(event);
        }
    }
```

---

## Database Schema Isolation

Each service has its own database:

```
Service         │ Database             │ Key Tables
────────────────┼──────────────────────┼─────────────────────────
Auth Service    │ auth_db              │ users, revoked_tokens, 
                │                      │ audit_logs, departments
────────────────┼──────────────────────┼─────────────────────────
Citizen Svc     │ citizen_db           │ citizens, citizen_docs,
                │                      │ citizen_addresses
────────────────┼──────────────────────┼─────────────────────────
Service Req Svc │ servicerequest_db    │ services, requests,
                │                      │ request_docs, requests_status
────────────────┼──────────────────────┼─────────────────────────
Permit Svc      │ permit_db            │ permits, permit_docs,
                │                      │ inspections
────────────────┼──────────────────────┼─────────────────────────
Grievance Svc   │ grievance_db         │ grievances, grievance_status,
                │                      │ grievance_comments, escalations
────────────────┼──────────────────────┼─────────────────────────
Public Works    │ publicworks_db       │ work_orders, milestones,
                │                      │ budgets, resources
────────────────┼──────────────────────┼─────────────────────────
Notification    │ notification_db      │ notifications, templates,
                │                      │ delivery_logs, preferences
────────────────┼──────────────────────┼─────────────────────────
Analytics Svc   │ analytics_db         │ reports, dashboards,
                │                      │ metrics, aggregations
```

---

## Configuration Summary

### application.yml (Gateway)

```yaml
server:
  port: 9090

app:
  jwt:
    secret: civicdesk_hs256_secret_key_minimum_32_characters_required

spring:
  # JWT validation with shared secret
  # Redis for distributed rate limiting
  # Circuit breaker per service
  # Tracing/observability
  
gateway:
  # Public paths (no auth required)
  # Rate limit thresholds
  # Circuit breaker settings
  
management:
  # Metrics export (Prometheus)
  # Health endpoints
  # Tracing (OpenTelemetry/Jaeger)
```

---

## Monitoring Checklist

- [ ] Prometheus scraping gateway metrics every 15s
- [ ] Grafana dashboard showing latency, error rate, circuit breaker status
- [ ] Jaeger receiving spans from gateway + all services
- [ ] Logs aggregated with trace IDs (ELK/Splunk)
- [ ] Alerts configured for:
  - Circuit breaker OPEN
  - Error rate > 5%
  - Latency p95 > 2s
  - Rate limit violations > 100/min

---

## Deployment Topology (Multi-Instance)

```
┌─────────────────────────────────────────────────┐
│              Load Balancer (Nginx)              │
│              (Port 80/443)                      │
└────────┬────────────────────────────┬───────────┘
         │                            │
    ┌────▼──────┐              ┌──────▼────┐
    │ Gateway 1 │◄─── Redis ──│ Gateway 2 │
    │ (9090)    │   (shared)  │ (9090)    │
    └────┬──────┘              └──────┬────┘
         │                            │
         └────────────┬───────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
    ┌───▼──┐    ┌────▼───┐   ┌────▼───┐
    │ 8083 │    │ 8081   │   │ 8082   │
    │      │    │        │   │        │
    │Serv  │    │Auth    │   │Citizen │
    │Req   │    │        │   │        │
    └──────┘    └────────┘   └────────┘
```

**With Redis Distributed Rate Limiting**:
- All gateway instances share rate quota via Redis
- Revoked tokens cached in Redis
- True distributed resilience

---

## Summary

The CivicDesk API Gateway is a **production-grade, resilient, observable entry point** that:

1. **Authenticates** all requests with JWT (HS384) + signature validation
2. **Routes** to 8 microservices based on path predicates
3. **Rate-limits** to prevent abuse (global or per-client)
4. **Protects** downstream services with circuit breakers (fail-fast on outages)
5. **Traces** all requests end-to-end with trace IDs across services
6. **Exports** Prometheus metrics for Grafana dashboards
7. **Logs** structured output with correlation IDs

All components work together to provide a secure, fast, scalable microservices gateway.
