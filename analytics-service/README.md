# Analytics Service

A microservice for generating civic analytics reports and dashboards for CivicDesk.

## Overview

The Analytics Service provides analytics, reporting, and metrics capabilities for CivicDesk administrators. It integrates with:
- **Auth Service** (port 8081): JWT authentication and audit logging
- **Grievance Service** (port 8085): Grievance analytics data retrieval
- **API Gateway** (port 9090): External access routing

## Architecture

### Package Structure
```
com.civicdesk.analytics
├── config/              # Spring configurations (Security, Swagger, Beans)
├── controller/          # REST endpoints
├── service/             # Business logic layer
├── repository/          # Database access layer
├── entity/              # JPA entities
├── dto/                 # Data transfer objects
├── &#8470</entity>/          # Enumerations (AuditAction, AuditModule)
├── client/              # Inter-service communication clients
├── exception/           # Exception handling
├── response/            # Response wrapper classes
└── util/                # Utility classes
```

### Key Components

#### Controllers
- **ReportController** (`/civicDesk/analytics/reports`)
  - `POST /civicDesk/analytics/reports` - Generate a report
  - `GET /civicDesk/analytics/reports/user/{userId}` - Get reports by user
  - `GET /civicDesk/analytics/reports/{id}/download` - Download report as Excel
  - `DELETE /civicDesk/analytics/reports/{id}` - Soft delete a report

#### Services
- **IReportService** - Report generation and management interface
- **ReportServiceImpl** - Core business logic for analytics
- **ReportExportService** - Excel export functionality

#### Clients (Inter-Service Communication)
- **AuditClient** - Sends audit logs to auth-service at `/civicDesk/audit/auditLogs`
- **GrievanceClient** - Fetches grievance analytics from grievance-service

#### Security
- **SecurityConfig** - JWT-based authentication with Spring Security
- **JwtAuthFilter** - Extracts and validates JWT tokens
- **JwtTokenProvider** - JWT token validation
- **JwtUserContext** - Extracts user context from JWT

#### Database
- **CivicReport** Entity - Stores generated reports
- **CivicReportRepository** - JPA repository with custom queries

## Configuration

### Application Properties
```properties
server.port=8088
spring.application.name=analytics-service

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/civicdesk_analytics
spring.datasource.username=root
spring.datasource.password=root

# JWT Configuration (must match auth-service)
app.jwt.secret=civicdesk_hs256_secret_key_minimum_32_characters_required

# Service Endpoints
app.auth-service.url=http://localhost:8081
app.grievance-service.url=http://localhost:8085

# Swagger/OpenAPI
springdoc.api-docs.path=/civicDesk/api-docs
springdoc.swagger-ui.path=/civicDesk/swagger-ui.html
```

### Security Setup
All endpoints except Swagger/OpenAPI docs require a valid JWT token in the `Authorization: Bearer <token>` header.

Public paths:
- `/civicDesk/swagger-ui/**`
- `/civicDesk/swagger-ui.html`
- `/civicDesk/api-docs/**`
- `/v3/api-docs/**`

## API Security

### Authentication
Obtain a JWT token from auth-service:
```bash
POST /civicDesk/iam/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password"
}
```

Include token in requests:
```bash
Authorization: Bearer <jwt-token>
```

### Role-Based Access
- `ROLE_COMPLIANCE` - View reports and metrics
- `ROLE_DEPT_SUPERVISOR` - View department-specific reports
- `ROLE_ADMIN` - Full report access and management

## API Endpoints

### Generate Report
```http
POST /civicDesk/analytics/reports
Authorization: Bearer <token>
Content-Type: application/json

{
  "type": "GRIEVANCE",
  "departmentId": "DPT01",
  "fromDate": "2026-01-01T00:00:00",
  "toDate": "2026-12-31T23:59:59"
}

Response (201):
{
  "success": true,
  "message": "Report is ready",
  "data": {
    "reportId": "uuid",
    "reportType": "GRIEVANCE",
    "generatedDate": "2026-07-07T16:44:00",
    "status": "GENERATED"
  }
}
```

### Get Reports by User
```http
GET /civicDesk/analytics/reports/user/{userId}
Authorization: Bearer <token>

Response (200):
{
  "success": true,
  "data": {
    "reports": [...],
    "count": 5
  }
}
```

### Download Report
```http
GET /civicDesk/analytics/reports/{reportId}/download
Authorization: Bearer <token>

Response (200): Excel file binary data
```

### Delete Report
```http
DELETE /civicDesk/analytics/reports/{reportId}
Authorization: Bearer <token>

Response (200):
{
  "success": true,
  "message": "Report deleted successfully"
}
```

## Audit Logging

All report operations are logged via audit trail integration with auth-service:
- **GENERATE_REPORT** - When a report is generated
- **DOWNLOAD_REPORT** - When a report is downloaded
- **DELETE_REPORT** - When a report is deleted

Audit logs include:
- User ID
- Action taken
- Module (ANALYTICS)
- Client IP Address
- Timestamp

## Database Setup

Automatically created tables:
- `civic_reports` - Stores report snapshots with metrics

The service creates the database and tables automatically during startup using Hibernate's `ddl-auto=update` setting.

## Build & Deploy

### Build
```bash
cd analytics-service
../civicdesk/mvnw clean package -DskipTests
```

### Run Standalone
```bash
java -jar target/analytics-service-1.0.0.jar
```

### Run with Docker
```bash
docker build -t civicdesk/analytics-service:1.0.0 .
docker run -p 8088:8088 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/civicdesk_analytics \
  -e APP_AUTH_SERVICE_URL=http://auth-service:8081 \
  -e APP_GRIEVANCE_SERVICE_URL=http://grievance-service:8085 \
  --network civicdesk-net \
  civicdesk/analytics-service:1.0.0
```

## Integration with API Gateway

The analytics-service is configured in the API Gateway at route `/civicDesk/analytics/**`:

```yaml
# From api-gateway/src/main/resources/application.yml
- id: analytics-service
  uri: http://localhost:8088
  predicates:
    - Path=/civicDesk/analytics/**

- id: analytics-service-docs
  uri: http://localhost:8088
  predicates:
    - Path=/analytics-service/api-docs
  filters:
    - RewritePath=/analytics-service/api-docs, /civicDesk/api-docs
```

## Inter-Service Communication

### Calling Other Services

**AuditClient** - Log actions to auth-service:
```java
auditClient.logAudit(userId, AuditAction.GENERATE_REPORT.name(), 
                     AuditModule.ANALYTICS.name(), clientIp);
```

**GrievanceClient** - Fetch analytics from grievance-service:
```java
GrievanceAnalyticsResponse analytics = grievanceClient.getGrievanceAnalytics(request);
```

## Error Handling

Global exception handler provides consistent error responses:

```json
{
  "success": false,
  "error": "Error message description"
}
```

Common HTTP Status Codes:
- `200 OK` - Successful operation
- `201 Created` - Report generated successfully
- `400 Bad Request` - Invalid request parameters
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Report not found
- `500 Internal Server Error` - Server-side error

## Logging

Configured via `logback-spring.xml`:
- Console output with timestamp, thread, level, logger, and message
- Rotating file appender: `logs/analytics-service/analytics-service.log`
- Daily rotation with 30-day retention

Configure log levels in `application.properties`:
```properties
logging.level.com.civicdesk.analytics=DEBUG
logging.level.org.springframework.security=INFO
```

## Testing

Run tests with:
```bash
cd analytics-service
../civicdesk/mvnw test
```

## Dependencies

Key Maven dependencies:
- `spring-boot-starter-web` - Web framework
- `spring-boot-starter-data-jpa` - Database ORM
- `spring-boot-starter-security` - Security framework
- `jjwt` - JWT token handling
- `springdoc-openapi-starter-webmvc-ui` - Swagger/OpenAPI
- `poi-ooxml` - Excel export
- `mysql-connector-j` - MySQL database driver
- `lombok` - Code generation
